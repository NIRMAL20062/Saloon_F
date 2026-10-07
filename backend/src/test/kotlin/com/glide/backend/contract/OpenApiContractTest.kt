package com.glide.backend.contract

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport
import com.glide.backend.TestTokens
import com.glide.backend.db.ExposedTransactor
import com.glide.backend.db.TestDatabase
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.salons.ExposedSalonRepository
import com.glide.backend.testConfig
import com.glide.backend.users.ExposedUserRepository
import com.glide.shared.api.ApiRoutes
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.testing.testApplication
import io.swagger.v3.parser.OpenAPIV3Parser
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real backend responses must match docs/api/openapi.yaml exactly, and every backend route must be documented there.
 * The admin panel's TypeScript types are generated from that file, so drift here would break the panel silently.
 */
class OpenApiContractTest {
    private val specPath = System.getProperty("openapi.spec") ?: error("openapi.spec system property not set")
    private val validator = OpenApiInteractionValidator.createForSpecificationUrl(specPath).build()

    @Test
    fun `liveness response matches the spec`() =
        testApplication {
            application { module(testConfig(), fakeDependencies()) }

            assertMatchesSpec(ApiRoutes.HEALTH_LIVE, client.get(ApiRoutes.HEALTH_LIVE))
        }

    @Test
    fun `readiness UP response matches the spec`() =
        testApplication {
            application { module(testConfig(), fakeDependencies(databaseHealthy = true)) }

            assertMatchesSpec(ApiRoutes.HEALTH, client.get(ApiRoutes.HEALTH))
        }

    @Test
    fun `readiness DOWN response matches the spec`() =
        testApplication {
            application { module(testConfig(), fakeDependencies(databaseHealthy = false)) }

            assertMatchesSpec(ApiRoutes.HEALTH, client.get(ApiRoutes.HEALTH))
        }

    @Test
    fun `rate-limited error envelope matches the spec`() =
        testApplication {
            application { module(testConfig(rateLimitPerMinute = 1), fakeDependencies()) }
            client.get(ApiRoutes.HEALTH_LIVE)

            val limited = client.get(ApiRoutes.HEALTH_LIVE)

            assertEquals(429, limited.status.value)
            assertMatchesSpec(ApiRoutes.HEALTH_LIVE, limited)
        }

    @Test
    fun `me response matches the spec`() =
        testApplication {
            application { module(testConfig(), fakeDependencies()) }

            assertMatchesSpec(ApiRoutes.ME, client.get(ApiRoutes.ME) { bearerAuth(TestTokens.token()) })
        }

    @Test
    fun `unauthorized envelope matches the spec`() =
        testApplication {
            application { module(testConfig(), fakeDependencies()) }

            val response = client.get(ApiRoutes.ME)

            assertEquals(401, response.status.value)
            assertMatchesSpec(ApiRoutes.ME, response)
        }

    @Test
    fun `admin responses match the spec`() =
        testApplication {
            val deps = fakeDependencies()
            val adminId = UUID.randomUUID()
            runBlocking { deps.admins.addFirst(adminId, "first@glide.test") }
            application { module(testConfig(), deps) }
            val admin = TestTokens.adminToken(adminId, "first@glide.test")

            assertMatchesSpec(ApiRoutes.ADMIN_ME, client.get(ApiRoutes.ADMIN_ME) { bearerAuth(admin) })
            assertMatchesSpec(
                ApiRoutes.ADMIN_ME,
                client.get(
                    ApiRoutes.ADMIN_ME,
                ) { bearerAuth(TestTokens.adminToken(adminId, "first@glide.test", mfa = false)) },
            )
            val invite = { body: String ->
                runBlocking {
                    client.post(ApiRoutes.ADMIN_INVITES) {
                        bearerAuth(admin)
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
            }
            listOf(
                invite("""{"email":"b@glide.test"}""") to 201,
                invite("""{"email":"b@glide.test"}""") to 409,
                invite("""{"email":"nope"}""") to 400,
            ).forEach { (response, status) ->
                assertEquals(status, response.status.value)
                assertMatchesSpec(ApiRoutes.ADMIN_INVITES, response, Request.Method.POST)
            }
        }

    @Test
    fun `salon responses match the spec`() =
        testApplication {
            TestDatabase.dataSource // migrated
            val deps =
                fakeDependencies().copy(
                    transactor = ExposedTransactor(TestDatabase.exposed),
                    users = ExposedUserRepository(),
                    salons = ExposedSalonRepository(),
                )
            application { module(testConfig(database = TestDatabase.config), deps) }
            val token = TestTokens.token(phone = "91" + (7_000_000_000L + (System.nanoTime() % 999_999_999L)))
            val send = { method: HttpMethod, path: String, body: String? ->
                runBlocking {
                    client.request(path) {
                        this.method = method
                        bearerAuth(token)
                        if (body != null) {
                            contentType(ContentType.Application.Json)
                            setBody(body)
                        }
                    }
                }
            }
            val profile =
                """
                {"name":"Glow","address":{"line1":"1 Road","area":"Area","city":"Pune","state":"MAHARASHTRA",
                 "pincode":"411001"},"type":"MEN"}
                """.trimIndent()
            val check = { method: HttpMethod, path: String, body: String?, status: Int ->
                runBlocking {
                    val response = send(method, path, body)
                    assertEquals(status, response.status.value, response.bodyAsText())
                    assertMatchesSpec(path, response, Request.Method.valueOf(method.value))
                }
            }

            check(HttpMethod.Post, ApiRoutes.SALON_SALONS, profile, 403) // the salon side isn't chosen yet
            check(HttpMethod.Get, ApiRoutes.SALON_ME, null, 404)
            send(HttpMethod.Put, ApiRoutes.ME_SIDE, """{"side":"SALON"}""")
            check(HttpMethod.Post, ApiRoutes.SALON_SALONS, profile.replace("Glow", ""), 400)
            check(HttpMethod.Post, ApiRoutes.SALON_SALONS, profile, 201)
            check(HttpMethod.Post, ApiRoutes.SALON_SALONS, profile, 409)
            check(HttpMethod.Get, ApiRoutes.SALON_ME, null, 200)
            check(HttpMethod.Put, ApiRoutes.SALON_PROFILE, profile, 200)

            // Bank details and submitting (BE-032).
            val bank = """{"accountHolderName":"Asha","accountNumber":"50100123456789","ifsc":"HDFC0001234"}"""
            check(HttpMethod.Get, ApiRoutes.SALON_BANK_DETAILS, null, 404)
            check(HttpMethod.Post, ApiRoutes.SALON_SUBMIT, null, 409)
            check(HttpMethod.Put, ApiRoutes.SALON_BANK_DETAILS, bank.replace("HDFC0001234", "nope"), 400)
            check(HttpMethod.Put, ApiRoutes.SALON_BANK_DETAILS, bank, 200)
            check(HttpMethod.Get, ApiRoutes.SALON_BANK_DETAILS, null, 200)
            check(HttpMethod.Post, ApiRoutes.SALON_SUBMIT, null, 200)
        }

    @Test
    fun `the validator rejects a body that breaks the spec`() {
        // Guards the guard: if this passes, the contract test above is actually checking something.
        val report = validate(ApiRoutes.HEALTH, 200, """{"status":"MAYBE","version":"1","surprise":true}""")

        assertTrue(report.hasErrors(), "validator accepted an invalid body")
    }

    @Test
    fun `every backend route is documented in the spec`() {
        var registered = emptySet<String>()
        testApplication {
            application {
                module(testConfig(), fakeDependencies())
                registered = documentedShape(this)
            }
            startApplication()
        }
        val documented =
            OpenAPIV3Parser()
                .read(File(specPath).toURI().toString())
                .paths
                .flatMap { (path, item) -> item.readOperationsMap().keys.map { "${it.name} $path" } }
                .toSet()

        assertEquals(emptySet(), registered - documented, "routes missing from docs/api/openapi.yaml")
        assertEquals(emptySet(), documented - registered, "spec documents routes the backend doesn't have")
    }

    private suspend fun assertMatchesSpec(
        path: String,
        response: HttpResponse,
        method: Request.Method = Request.Method.GET,
    ) {
        val report =
            validate(
                path,
                response.status.value,
                response.bodyAsText(),
                response.headers[HttpHeaders.ContentType],
                method,
            )
        assertTrue(
            !report.hasErrors(),
            "Response for $method $path does not match openapi.yaml:\n" + report.messages.joinToString("\n"),
        )
    }

    private fun validate(
        path: String,
        status: Int,
        body: String,
        contentType: String? = "application/json",
        method: Request.Method = Request.Method.GET,
    ): ValidationReport =
        validator.validateResponse(
            path,
            method,
            SimpleResponse.Builder
                .status(status)
                .withContentType(contentType ?: "application/json")
                .withBody(body)
                .build(),
        )

    /** "GET /health" style strings for every route the backend registers. */
    private fun documentedShape(application: Application): Set<String> =
        application
            .plugin(RoutingRoot)
            .getAllRoutes()
            .mapNotNull { node: RoutingNode ->
                val method = (node.selector as? HttpMethodRouteSelector)?.method ?: return@mapNotNull null
                "${method.value} ${node.parent?.path()}"
            }.toSet()

    private fun RoutingNode.path(): String =
        generateSequence(this) { it.parent }
            .toList()
            .reversed()
            .mapNotNull { it.selector.toString().takeIf { s -> s.isNotEmpty() && !s.startsWith("(") } }
            .joinToString(separator = "/", prefix = "/")
}

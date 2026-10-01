package com.glide.backend.contract

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.shared.api.ApiRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.testing.testApplication
import io.swagger.v3.parser.OpenAPIV3Parser
import java.io.File
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
    ) {
        val report =
            validate(path, response.status.value, response.bodyAsText(), response.headers[HttpHeaders.ContentType])
        assertTrue(
            !report.hasErrors(),
            "Response for GET $path does not match openapi.yaml:\n" + report.messages.joinToString("\n"),
        )
    }

    private fun validate(
        path: String,
        status: Int,
        body: String,
        contentType: String? = "application/json",
    ): ValidationReport =
        validator.validateResponse(
            path,
            Request.Method.GET,
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

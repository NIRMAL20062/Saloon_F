package com.glide.backend.health

import com.glide.backend.AppDependencies
import com.glide.backend.db.TestDatabase
import com.glide.backend.fakeDependencies
import com.glide.backend.module
import com.glide.backend.testConfig
import com.glide.shared.api.ApiJson
import com.glide.shared.api.ApiRoutes
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.postgresql.ds.PGSimpleDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadinessRouteTest {
    @Test
    fun `reports UP with 200 when the database answers`() =
        testApplication {
            application { module(testConfig(), fakeDependencies(databaseHealthy = true)) }

            val response = client.get(ApiRoutes.HEALTH)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(HealthResponse(HealthStatus.UP, "test", HealthStatus.UP), response.health())
        }

    @Test
    fun `reports DOWN with 503 when the database does not answer`() =
        testApplication {
            application { module(testConfig(), fakeDependencies(databaseHealthy = false)) }

            val response = client.get(ApiRoutes.HEALTH)

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(HealthResponse(HealthStatus.DOWN, "test", HealthStatus.DOWN), response.health())
        }

    @Test
    fun `real check against a real PostgreSQL reports UP`() =
        testApplication {
            val deps = AppDependencies(JdbcDatabaseHealthCheck(TestDatabase.dataSource))
            application { module(testConfig(database = TestDatabase.config), deps) }

            val response = client.get(ApiRoutes.HEALTH)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(HealthStatus.UP, response.health().database)
        }

    @Test
    fun `real check against an unreachable database reports DOWN`() =
        testApplication {
            val unreachable = PGSimpleDataSource().apply { setURL("jdbc:postgresql://127.0.0.1:1/none") }
            val deps = AppDependencies(JdbcDatabaseHealthCheck(unreachable))
            application { module(testConfig(), deps) }

            val response = client.get(ApiRoutes.HEALTH)

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(HealthStatus.DOWN, response.health().database)
        }

    private suspend fun HttpResponse.health() = ApiJson.decodeFromString<HealthResponse>(bodyAsText())
}

package com.glide.backend

import com.glide.backend.auth.SupabaseTokenVerifier
import com.glide.backend.auth.TokenVerifier
import com.glide.backend.auth.configureAuthentication
import com.glide.backend.config.AppConfig
import com.glide.backend.db.DatabaseFactory
import com.glide.backend.health.DatabaseHealthCheck
import com.glide.backend.health.JdbcDatabaseHealthCheck
import com.glide.backend.health.healthRoutes
import com.glide.backend.plugins.configureErrorHandling
import com.glide.backend.plugins.configureMonitoring
import com.glide.backend.plugins.configureSecurity
import com.glide.backend.plugins.configureSerialization
import com.glide.backend.users.ExposedUserRepository
import com.glide.backend.users.UserRepository
import com.glide.backend.users.meRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import java.util.Properties

fun main() {
    val config = AppConfig.fromEnv(System.getenv(), readBuildVersion())
    val dataSource = DatabaseFactory.createDataSource(config.database)
    DatabaseFactory.migrate(dataSource)
    val database = DatabaseFactory.connectExposed(dataSource)
    val dependencies =
        AppDependencies(
            databaseHealthCheck = JdbcDatabaseHealthCheck(dataSource),
            tokenVerifier = SupabaseTokenVerifier.forProject(config.supabase),
            users = ExposedUserRepository(database),
        )

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config, dependencies)
    }.start(wait = true)
}

/** Everything routes need that talks to the outside world. Tests pass fakes or test-database versions. */
data class AppDependencies(
    val databaseHealthCheck: DatabaseHealthCheck,
    val tokenVerifier: TokenVerifier,
    val users: UserRepository,
)

/** Wires every plugin and route. Tests call this directly with a test config. */
fun Application.module(
    config: AppConfig,
    dependencies: AppDependencies,
) {
    configureMonitoring()
    configureSecurity(config)
    configureErrorHandling()
    configureSerialization()
    configureAuthentication(dependencies.tokenVerifier)
    routing {
        healthRoutes(config.version, dependencies.databaseHealthCheck)
        meRoutes(dependencies.users)
    }
}

private fun readBuildVersion(): String =
    Properties()
        .apply { AppConfig::class.java.getResourceAsStream("/version.properties")?.use { load(it) } }
        .getProperty("version", "unknown")

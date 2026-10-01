package com.glide.backend

import com.glide.backend.config.AppConfig
import com.glide.backend.db.DatabaseFactory
import com.glide.backend.health.healthRoutes
import com.glide.backend.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import java.util.Properties

fun main() {
    val config = AppConfig.fromEnv(System.getenv(), readBuildVersion())
    val dataSource = DatabaseFactory.createDataSource(config.database)
    DatabaseFactory.migrate(dataSource)
    DatabaseFactory.connectExposed(dataSource)

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

/** Wires every plugin and route. Tests call this directly with a test config. */
fun Application.module(config: AppConfig) {
    configureSerialization()
    routing {
        healthRoutes(config.version)
    }
}

private fun readBuildVersion(): String =
    Properties()
        .apply { AppConfig::class.java.getResourceAsStream("/version.properties")?.use { load(it) } }
        .getProperty("version", "unknown")

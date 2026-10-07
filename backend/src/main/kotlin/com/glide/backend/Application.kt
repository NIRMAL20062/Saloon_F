package com.glide.backend

import com.glide.backend.admins.AdminRepository
import com.glide.backend.admins.AdminService
import com.glide.backend.admins.AuthAdmin
import com.glide.backend.admins.ExposedAdminRepository
import com.glide.backend.admins.SupabaseAuthAdmin
import com.glide.backend.admins.adminRoutes
import com.glide.backend.auth.SupabaseTokenVerifier
import com.glide.backend.auth.TokenVerifier
import com.glide.backend.auth.configureAuthentication
import com.glide.backend.config.AppConfig
import com.glide.backend.crypto.FieldCipher
import com.glide.backend.db.DatabaseFactory
import com.glide.backend.db.ExposedTransactor
import com.glide.backend.db.Transactor
import com.glide.backend.health.DatabaseHealthCheck
import com.glide.backend.health.JdbcDatabaseHealthCheck
import com.glide.backend.health.healthRoutes
import com.glide.backend.plugins.configureErrorHandling
import com.glide.backend.plugins.configureMonitoring
import com.glide.backend.plugins.configureSecurity
import com.glide.backend.plugins.configureSerialization
import com.glide.backend.salons.ExposedSalonRepository
import com.glide.backend.salons.SalonRepository
import com.glide.backend.salons.SalonService
import com.glide.backend.salons.salonRoutes
import com.glide.backend.users.ExposedUserRepository
import com.glide.backend.users.UserRepository
import com.glide.backend.users.UserService
import com.glide.backend.users.meRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.util.Properties

fun main() {
    val config = AppConfig.fromEnv(System.getenv(), readBuildVersion())
    if (config.supabase.secretKey == null) {
        LoggerFactory.getLogger("com.glide.backend").warn("SUPABASE_SECRET_KEY is not set: admin invites are off")
    }
    if (config.bankDetailsKey == null) {
        LoggerFactory.getLogger("com.glide.backend").warn("BANK_DETAILS_KEY is not set: salons can't save bank details")
    }
    DatabaseFactory.migrate(config.database)
    val dataSource = DatabaseFactory.createDataSource(config.database)
    val database = DatabaseFactory.connectExposed(dataSource)
    val dependencies =
        AppDependencies(
            databaseHealthCheck = JdbcDatabaseHealthCheck(dataSource),
            tokenVerifier = SupabaseTokenVerifier.forProject(config.supabase),
            transactor = ExposedTransactor(database),
            users = ExposedUserRepository(),
            admins = ExposedAdminRepository(),
            salons = ExposedSalonRepository(),
            authAdmin = SupabaseAuthAdmin.forProject(config.supabase),
        )

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config, dependencies)
    }.start(wait = true)
}

/** Everything routes need that talks to the outside world. Tests pass fakes or test-database versions. */
data class AppDependencies(
    val databaseHealthCheck: DatabaseHealthCheck,
    val tokenVerifier: TokenVerifier,
    /** Services run each piece of work in one transaction through it (BE-025). */
    val transactor: Transactor,
    val users: UserRepository,
    val admins: AdminRepository,
    val salons: SalonRepository,
    /** Supabase's admin API (secret key). Without the key: DisabledAuthAdmin, and invites answer 503. */
    val authAdmin: AuthAdmin,
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
        meRoutes(UserService(dependencies.transactor, dependencies.users))
        adminRoutes(AdminService(dependencies.transactor, dependencies.admins, dependencies.authAdmin))
        salonRoutes(
            SalonService(
                dependencies.transactor,
                dependencies.users,
                dependencies.salons,
                config.bankDetailsKey?.let(::FieldCipher),
            ),
        )
    }
}

private fun readBuildVersion(): String =
    Properties()
        .apply { AppConfig::class.java.getResourceAsStream("/version.properties")?.use { load(it) } }
        .getProperty("version", "unknown")

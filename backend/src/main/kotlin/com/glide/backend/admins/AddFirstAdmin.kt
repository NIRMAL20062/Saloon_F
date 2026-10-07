package com.glide.backend.admins

import com.glide.backend.config.AppConfig
import com.glide.backend.db.DatabaseFactory
import com.glide.backend.db.ExposedTransactor
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * One-off command (BE-020): makes the first admin of our team. Every later admin is invited from the admin website.
 *
 * `./gradlew :backend:addFirstAdmin --args="admin@example.com"` (reads the repo-root .env, like `:backend:run`).
 * Needs SUPABASE_SECRET_KEY. Sends no email: the person logs in to the admin website with their email, then sets up
 * the authenticator app.
 */
fun main(args: Array<String>) {
    val config = AppConfig.fromEnv(System.getenv(), version = "command")
    DatabaseFactory.migrate(config.database)
    val dataSource = DatabaseFactory.createDataSource(config.database)
    val (exitCode, message) =
        dataSource.use {
            val transactor = ExposedTransactor(DatabaseFactory.connectExposed(it))
            val service =
                AdminService(transactor, ExposedAdminRepository(), SupabaseAuthAdmin.forProject(config.supabase))
            runBlocking { addFirstAdmin(args, service) }
        }
    println(message)
    exitProcess(exitCode)
}

/** The command without the wiring, so tests can run it with fakes. Returns the exit code and what to print. */
suspend fun addFirstAdmin(
    args: Array<String>,
    service: AdminService,
): Pair<Int, String> {
    val email =
        args.singleOrNull() ?: return 2 to "Usage: ./gradlew :backend:addFirstAdmin --args=\"admin@example.com\""
    return when (val outcome = service.addFirst(email)) {
        is AdminService.Outcome.Added -> {
            0 to "Added ${outcome.admin.email} as the first admin. They log in to the admin website with this email, " +
                "then set up an authenticator app."
        }

        AdminService.Outcome.InvalidEmail -> {
            2 to "That doesn't look like an email address."
        }

        AdminService.Outcome.AlreadyExists -> {
            1 to "There is already a first admin. Ask an admin to invite more admins from the admin website."
        }

        AdminService.Outcome.Unavailable -> {
            1 to "SUPABASE_SECRET_KEY is not set in .env (Supabase → Project Settings → API Keys → Secret keys)."
        }

        AdminService.Outcome.SupabaseFailed -> {
            1 to "Supabase refused or didn't answer (details in the log above). Nothing was saved; try again."
        }
    }
}

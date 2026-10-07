package com.glide.backend.db

import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.UUID

/**
 * Who the current transaction works for, read by the row-level security policies (D-027, BE-031). Each setting lasts
 * until the end of the transaction only, so it never leaks to the next user of a pooled connection.
 * The salon always comes from the signed-in person's membership (D-036), never from a request.
 */
object RowSecurity {
    /** Rows of this salon become visible and writable. */
    fun forSalon(salon: UUID) = set("app.salon_id", salon)

    /** The person's own rows become visible (e.g. their membership, to find their salon). */
    fun forUser(user: UUID) = set("app.user_id", user)

    private fun set(
        setting: String,
        value: UUID,
    ) {
        TransactionManager.current().exec(
            "SELECT set_config(?, ?, true)",
            listOf(TextColumnType() to setting, TextColumnType() to value.toString()),
        ) { }
    }
}

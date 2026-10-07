package com.glide.backend.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction as exposedTransaction

/**
 * Runs one piece of work in one database transaction (BE-025). **Services own the transaction**: everything a use case
 * reads and writes goes in one [transaction] block, so "check, then write" is one step, and the per-transaction settings
 * that row-level security needs (D-027) have one place to go. Repositories never open their own; Exposed refuses their
 * queries outside a transaction. The block can't suspend, so nothing slow (a call to Supabase) runs while it is open.
 */
interface Transactor {
    /**
     * [salon]: the salon this piece of work is for (from the signed-in person's membership, D-036, never from the request);
     * row-level security then shows and accepts only that salon's rows (BE-031, D-027). Null: no salon-owned row at all.
     * [user]: the signed-in person, whose own rows (their membership) become visible. Both last for this transaction only;
     * see [RowSecurity] to set the salon once it is known inside the transaction.
     */
    suspend fun <T> transaction(
        salon: UUID? = null,
        user: UUID? = null,
        block: () -> T,
    ): T
}

class ExposedTransactor(
    private val database: Database,
) : Transactor {
    override suspend fun <T> transaction(
        salon: UUID?,
        user: UUID?,
        block: () -> T,
    ): T =
        withContext(Dispatchers.IO) {
            exposedTransaction(database) {
                salon?.let(RowSecurity::forSalon)
                user?.let(RowSecurity::forUser)
                block()
            }
        }
}

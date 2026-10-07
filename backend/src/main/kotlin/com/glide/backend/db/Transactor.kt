package com.glide.backend.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction as exposedTransaction

/**
 * Runs one piece of work in one database transaction (BE-025). **Services own the transaction**: everything a use case
 * reads and writes goes in one [transaction] block, so "check, then write" is one step, and the per-transaction settings
 * that row-level security needs (D-027) have one place to go. Repositories never open their own; Exposed refuses their
 * queries outside a transaction. The block can't suspend, so nothing slow (a call to Supabase) runs while it is open.
 */
interface Transactor {
    suspend fun <T> transaction(block: () -> T): T
}

class ExposedTransactor(
    private val database: Database,
) : Transactor {
    override suspend fun <T> transaction(block: () -> T): T =
        withContext(Dispatchers.IO) { exposedTransaction(database) { block() } }
}

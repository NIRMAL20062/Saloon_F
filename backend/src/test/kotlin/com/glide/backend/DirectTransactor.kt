package com.glide.backend

import com.glide.backend.db.Transactor

/** For route tests with in-memory repositories: there is no database, so the block simply runs. */
object DirectTransactor : Transactor {
    override suspend fun <T> transaction(block: () -> T): T = block()
}

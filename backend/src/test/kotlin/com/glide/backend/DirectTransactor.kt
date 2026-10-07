package com.glide.backend

import com.glide.backend.db.Transactor
import java.util.UUID

/** For route tests with in-memory repositories: there is no database, so the block simply runs. */
object DirectTransactor : Transactor {
    override suspend fun <T> transaction(
        salon: UUID?,
        user: UUID?,
        block: () -> T,
    ): T = block()
}

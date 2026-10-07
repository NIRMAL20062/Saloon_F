package com.glide.backend.db

import com.glide.backend.users.AppUsers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/** One transaction per piece of work (BE-025), on real PostgreSQL. */
@OptIn(ExperimentalUuidApi::class)
class TransactorTest {
    private val transactor = ExposedTransactor(TestDatabase.exposed)

    @Test
    fun `everything in one block is kept together`() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()

        runBlocking {
            transactor.transaction {
                insertUser(a)
                insertUser(b)
            }
        }

        assertEquals(2, countUsers(a, b))
    }

    @Test
    fun `when a later write fails, the earlier ones are not kept either`() {
        val a = UUID.randomUUID()

        assertFailsWith<IllegalStateException> {
            runBlocking {
                transactor.transaction {
                    insertUser(a)
                    error("the second write failed")
                }
            }
        }

        assertEquals(0, countUsers(a))
    }

    @Test
    fun `a query outside a transaction is refused`() {
        assertFailsWith<IllegalStateException> { insertUser(UUID.randomUUID()) }
    }

    private fun insertUser(id: UUID) {
        AppUsers.insert { it[AppUsers.id] = id.toKotlinUuid() }
    }

    private fun countUsers(vararg ids: UUID): Long =
        runBlocking {
            transactor.transaction {
                AppUsers
                    .selectAll()
                    .where { AppUsers.id inList ids.map { it.toKotlinUuid() } }
                    .count()
            }
        }
}

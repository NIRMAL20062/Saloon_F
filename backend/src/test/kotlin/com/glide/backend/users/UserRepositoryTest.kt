package com.glide.backend.users

import com.glide.backend.auth.AuthenticatedUser
import com.glide.backend.db.ExposedTransactor
import com.glide.backend.db.TestDatabase
import com.glide.shared.me.UserSide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Real PostgreSQL (Testcontainers) with the real migrations. */
class UserRepositoryTest {
    private val repository = ExposedUserRepository()
    private val transactor = ExposedTransactor(TestDatabase.exposed)

    private suspend fun <T> tx(block: () -> T): T = transactor.transaction(block = block)

    @Test
    fun `first request creates the user with no side yet`() =
        runBlocking {
            val id = UUID.randomUUID()

            val user = tx { repository.ensure(AuthenticatedUser(id, "919000000001")) }

            assertEquals(AppUser(id, "919000000001", null), user)
            assertEquals(1, rowCount(id))
        }

    @Test
    fun `parallel first requests create exactly one row`() =
        runBlocking {
            val id = UUID.randomUUID()

            val user = AuthenticatedUser(id, "919000000002")

            (1..10).map { async(Dispatchers.IO) { tx { repository.ensure(user) } } }.awaitAll()

            assertEquals(1, rowCount(id))
        }

    @Test
    fun `phone changed in Supabase is synced, a missing phone keeps the stored one`() =
        runBlocking {
            val id = UUID.randomUUID()
            tx { repository.ensure(AuthenticatedUser(id, "919000000003")) }

            assertEquals("919000000009", tx { repository.ensure(AuthenticatedUser(id, "919000000009")) }.phone)
            assertEquals("919000000009", tx { repository.ensure(AuthenticatedUser(id, null)) }.phone)
        }

    @Test
    fun `the side is saved once and is final`() =
        runBlocking {
            val id = UUID.randomUUID()
            tx { repository.ensure(AuthenticatedUser(id, "919000000004")) }

            assertEquals(UserSide.SALON, tx { repository.chooseSide(id, UserSide.SALON) }?.side)
            // The same answer again: fine.
            assertEquals(UserSide.SALON, tx { repository.chooseSide(id, UserSide.SALON) }?.side)
            assertNull(tx { repository.chooseSide(id, UserSide.CUSTOMER) }) // a switch: refused
            assertEquals(UserSide.SALON, tx { repository.ensure(AuthenticatedUser(id, "919000000004")) }.side)
        }

    @Test
    fun `the database itself refuses to change a chosen side`() {
        val id = UUID.randomUUID()
        runBlocking { tx { repository.ensure(AuthenticatedUser(id, "919000000005")) } }
        runBlocking { tx { repository.chooseSide(id, UserSide.CUSTOMER) } }

        assertFailsWith<SQLException> {
            TestDatabase.dataSource.connection.use { conn ->
                conn.prepareStatement("UPDATE app_users SET side = 'SALON' WHERE id = ?").use {
                    it.setObject(1, id)
                    it.executeUpdate()
                }
            }
        }
    }

    @Test
    fun `the profile is saved and read back from postgres`() =
        runBlocking {
            val id = UUID.randomUUID()
            tx { repository.ensure(AuthenticatedUser(id, "919000000020")) }

            tx { repository.updateProfile(id, ProfileInput.Valid("Meera", "meera@example.com")) }

            val read = tx { repository.ensure(AuthenticatedUser(id, "919000000020")) }
            assertEquals("Meera" to "meera@example.com", read.name to read.email)
        }

    @Test
    fun `the database itself refuses a one-letter name or a malformed email`() {
        val id = UUID.randomUUID()
        runBlocking { tx { repository.ensure(AuthenticatedUser(id, "919000000021")) } }

        listOf(
            "UPDATE app_users SET name = 'A' WHERE id = ?",
            "UPDATE app_users SET email = 'not-an-email' WHERE id = ?",
        ).forEach { sql ->
            assertFailsWith<SQLException>(sql) {
                TestDatabase.dataSource.connection.use { conn ->
                    conn.prepareStatement(sql).use {
                        it.setObject(1, id)
                        it.executeUpdate()
                    }
                }
            }
        }
    }

    @Test
    fun `the database itself rejects an unknown side`() {
        assertFailsWith<SQLException> {
            TestDatabase.dataSource.connection.use { conn ->
                conn.prepareStatement("INSERT INTO app_users (id, side) VALUES (?, 'ADMIN')").use {
                    it.setObject(1, UUID.randomUUID())
                    it.executeUpdate()
                }
            }
        }
    }

    private fun rowCount(id: UUID): Int =
        TestDatabase.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT count(*) FROM app_users WHERE id = ?").use {
                it.setObject(1, id)
                it.executeQuery().use { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }
        }
}

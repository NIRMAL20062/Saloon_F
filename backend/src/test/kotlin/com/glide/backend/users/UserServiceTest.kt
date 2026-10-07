package com.glide.backend.users

import com.glide.backend.auth.AuthenticatedUser
import com.glide.backend.db.ExposedTransactor
import com.glide.backend.db.TestDatabase
import com.glide.shared.me.UserSide
import kotlinx.coroutines.runBlocking
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Each of the service's methods is one transaction (BE-025), on real PostgreSQL. */
class UserServiceTest {
    private val service = UserService(ExposedTransactor(TestDatabase.exposed), ExposedUserRepository())

    @Test
    fun `a first request that saves the profile keeps neither write when the second fails`() {
        val id = UUID.randomUUID()

        // Creating the row works; the database then refuses a one-letter name (V4), so the new row goes too.
        assertFailsWith<SQLException> {
            runBlocking { service.updateProfile(AuthenticatedUser(id, "919000000030"), ProfileInput.Valid("A", null)) }
        }

        assertEquals(0, rowCount(id))
    }

    @Test
    fun `a first request that chooses the side creates the row and saves the side together`() {
        val id = UUID.randomUUID()

        val saved = runBlocking { service.chooseSide(AuthenticatedUser(id, "919000000031"), UserSide.CUSTOMER) }

        assertEquals(UserSide.CUSTOMER, saved?.side)
        assertEquals(1, rowCount(id))
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

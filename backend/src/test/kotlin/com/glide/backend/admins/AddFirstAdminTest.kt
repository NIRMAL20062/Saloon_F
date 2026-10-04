package com.glide.backend.admins

import com.glide.backend.FakeAuthAdmin
import com.glide.backend.db.TestDatabase
import com.glide.shared.admin.AdminStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The first-admin command against real PostgreSQL, with Supabase's admin API faked. */
class AddFirstAdminTest {
    private val admins = ExposedAdminRepository(TestDatabase.exposed)
    private val supabase = FakeAuthAdmin()
    private val service = AdminService(admins, supabase)

    @BeforeTest
    fun emptyAdmins() = resetAdmins()

    @Test
    fun `adds the first admin as INVITED, linked to their Supabase login, without sending an email`() =
        runBlocking {
            val (code, message) = addFirstAdmin(arrayOf(" First@Glide.test "), service)

            assertEquals(0, code)
            assertTrue(message.startsWith("Added first@glide.test as the first admin"), message)
            val admin = admins.findByEmail("first@glide.test")!!
            assertEquals(supabase.loginOf("first@glide.test"), admin.userId)
            assertEquals(AdminStatus.INVITED, admin.status)
            assertNull(admin.invitedBy)
            assertEquals(emptyList(), supabase.invitesSent)
        }

    @Test
    fun `refuses a second first admin`() =
        runBlocking {
            addFirstAdmin(arrayOf("first@glide.test"), service)

            val (code, message) = addFirstAdmin(arrayOf("second@glide.test"), service)

            assertEquals(1, code)
            assertTrue(message.startsWith("There is already a first admin"), message)
            assertNull(admins.findByEmail("second@glide.test"))
        }

    @Test
    fun `explains wrong usage, a bad email, a missing key and Supabase failing`() =
        runBlocking {
            assertEquals(2, addFirstAdmin(emptyArray(), service).first)
            assertEquals(2, addFirstAdmin(arrayOf("a@glide.test", "b@glide.test"), service).first)
            assertEquals(2, addFirstAdmin(arrayOf("not-an-email"), service).first)

            val noKey = addFirstAdmin(arrayOf("first@glide.test"), AdminService(admins, DisabledAuthAdmin))
            assertEquals(1, noKey.first)
            assertTrue(noKey.second.startsWith("SUPABASE_SECRET_KEY is not set"))

            supabase.failWith = AuthAdminException(status = 503, errorCode = null)
            assertEquals(1, addFirstAdmin(arrayOf("first@glide.test"), service).first)
            assertNull(admins.findByEmail("first@glide.test"))
        }
}

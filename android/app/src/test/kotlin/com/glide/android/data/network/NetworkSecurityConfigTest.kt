package com.glide.android.data.network

import com.glide.android.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the "HTTPS only, except localhost in debug" rule (DF-10) against accidental edits.
 * Even if a release build were given an http:// URL, the release config below would refuse to connect.
 */
class NetworkSecurityConfigTest {
    @Test
    fun `release config allows no cleartext at all`() {
        val config = parse("src/main/res/xml/network_security_config.xml")

        assertEquals("false", config.baseConfig().getAttribute("cleartextTrafficPermitted"))
        assertEquals(0, config.getElementsByTagName("domain-config").length)
    }

    @Test
    fun `debug config allows cleartext only to localhost`() {
        val config = parse("src/debug/res/xml/network_security_config.xml")

        assertEquals("false", config.baseConfig().getAttribute("cleartextTrafficPermitted"))
        val domains =
            config.getElementsByTagName("domain").let { list ->
                (0 until list.length).map { list.item(it).textContent }
            }
        assertEquals(setOf("127.0.0.1", "localhost"), domains.toSet())
    }

    @Test
    fun `api base url is https in release and localhost in debug`() {
        if (BuildConfig.DEBUG) {
            assertEquals("http://127.0.0.1:8080/", BuildConfig.API_BASE_URL)
        } else {
            assertTrue(BuildConfig.API_BASE_URL, BuildConfig.API_BASE_URL.startsWith("https://"))
        }
    }

    private fun parse(path: String): Element =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(File(path))
            .documentElement

    private fun Element.baseConfig() = getElementsByTagName("base-config").item(0) as Element
}

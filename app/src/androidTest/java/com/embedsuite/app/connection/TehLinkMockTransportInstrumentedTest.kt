package com.embedsuite.app.connection

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.embedsuite.app.security.TehLinkSecureSession
import com.embedsuite.app.security.SecureStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation integration: MockTransport firmware + TehLinkClient ping/handshake.
 */
@RunWith(AndroidJUnit4::class)
class TehLinkMockTransportInstrumentedTest {

    @Test
    fun mockTransportPingAndHandshake() = runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val transport = MockTransport()
        transport.openPairingWindow(120)
        val client = TehLinkClient(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
            secureSession = TehLinkSecureSession(SecureStore(context))
        )

        transport.connect().getOrThrow()
        val pong = client.ping(transport).getOrThrow()
        assertTrue(pong)

        val token = client.pair(transport).getOrThrow()
        client.authToken = token
        assertTrue(token.isNotBlank())

        val secure = client.establishSecureSession(transport)
        assertTrue(secure.isSuccess)
    }
}

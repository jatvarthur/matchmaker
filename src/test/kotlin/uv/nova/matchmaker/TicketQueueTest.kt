package uv.nova.matchmaker;

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.server.testing.*
import kotlin.test.Test

class TicketQueueKtTest {

    @Test
    fun testPostEmptyRoute() = testApplication {
        application {
            main()
        }
        client.post("").apply {
            TODO("Please write your test here")
        }
    }
}

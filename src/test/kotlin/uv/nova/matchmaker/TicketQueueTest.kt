package uv.nova.matchmaker;

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uv.nova.matchmaker.core.Ticket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TicketQueueTest {

    @Test
    fun testPostTicket() = test(mockk<TicketQueueService>()) { _, client, queueService ->
        every { queueService.enqeue(any()) } just Runs

        client.post("/queue") {
            contentType(ContentType.Application.Json)
            setBody(Json.parseToJsonElement("""{"playerId": 10, "skill":2650, "latency": 210}"""))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText())
            assertEquals(true, response.jsonObject["acknowledged"]?.jsonPrimitive?.booleanOrNull)
            assertTrue { bodyAsText().contains("acknowledged") }
        }

        verify(exactly = 1) { queueService.enqeue(any()) }
    }

    @Test
    fun testMutipleTickets() = test(TicketQueueService()) { _, client, queueService ->
        @Serializable
        data class TicketIn(val playerId: Long, val skill: Long, val latency: Long)

        (1L..10L).forEach { playerId ->
            client.post("/queue") {
                contentType(ContentType.Application.Json)
                setBody(TicketIn(playerId, 1234, 100))
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }

        val queue = queueService.getAndReset()
        assertEquals(10, queue.size)
        queue.forEachIndexed { i, ticket ->
            assertEquals((i + 1).toLong(), ticket.playerId)
            assertEquals(1234, ticket.skill)
            assertEquals(100, ticket.latency)
        }
    }


    @Test
    fun testGetAndReset() = test(TicketQueueService()) { _, _, queueService ->
        (1L .. 10L).forEach { playerId ->
            queueService.enqeue(Ticket(playerId, 1234, 100, listOf()))
        }

        val queue1 = queueService.getAndReset()
        assertEquals(10, queue1.size)

        val queue2 = queueService.getAndReset()
        assertEquals(0, queue2.size)
    }

    private fun test(queueService: TicketQueueService,
        block: suspend (builder: ApplicationTestBuilder, client: HttpClient, queueService: TicketQueueService) -> Unit
    ): Unit {
        return testApplication {
            application {
                installPlugins()
                routing {
                    ticketQueue(queueService)
                }
            }

            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            block(this, client, queueService)
        }
    }
}

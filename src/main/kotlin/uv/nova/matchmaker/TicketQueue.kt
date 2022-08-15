package uv.nova.matchmaker

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import uv.nova.matchmaker.core.Ticket
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class TicketQueueService {
    private var queue = mutableListOf<Ticket>()
    private val queueLock = ReentrantLock()

    fun enqeue(ticket: Ticket) {
        queueLock.withLock {
            queue.add(ticket)
        }
    }

    fun getAndReset(): List<Ticket> {
        return queueLock.withLock {
            val result = queue
            queue = mutableListOf()
            result
        }
    }
}

@Serializable
private data class TicketIn(
    val playerId: Long,
    val skill: Int,
    val latency: Int
)

private fun TicketIn.toTicket() = Ticket(playerId, skill, latency, listOf())

@Serializable
private data class PostTicketOut(
    val acknowledged: Boolean,
    val playerId: Long
)

fun Routing.ticketQueue(queueService: TicketQueueService) {

    route("/queue") {

        post<TicketIn> {ticketIn ->
            queueService.enqeue(ticketIn.toTicket())
            call.respond(PostTicketOut(true, ticketIn.playerId))
        }

    }
}






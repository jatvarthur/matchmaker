package uv.nova.matchmaker

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing


fun Application.installPlugins() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(ContentNegotiation) {
        json()
    }
}

val ticketQueueService = TicketQueueService()
val matchmakerService = MatchmakerService(ticketQueueService)

fun main() {
    matchmakerService.start()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        installPlugins()

        routing {
            ticketQueue(ticketQueueService)
        }
    }.start(wait = true)

    matchmakerService.shutdown()
}

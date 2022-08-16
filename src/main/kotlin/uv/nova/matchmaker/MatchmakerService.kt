package uv.nova.matchmaker

import uv.nova.matchmaker.core.Match
import uv.nova.matchmaker.core.Ticket
import uv.nova.matchmaker.core.VectorBuffer
import uv.nova.matchmaker.core.kmeans
import java.lang.Thread.currentThread
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.math.max


const val MATCHMAKING_PERIOD = 2 * 60 * 1000L
val CLUSTER_COUNT = intArrayOf(10, 100, 500, 1000, 5000, 10000)

class MatchmakerService(val queueService: TicketQueueService) {
    lateinit var worker: Thread

    fun start() {
        worker = thread(start = true, name = "matchmaker-worker-thread") {
            var delay = MATCHMAKING_PERIOD

            while (true) {
                sleep(delay)

                val timeStart = System.currentTimeMillis()
                val tickets = queueService.getAndReset()
                val clusterCount = selectClusterCount(tickets.size)
                val result = makeMatches(tickets, clusterCount)

                result.ticketsLeft.forEach { ticket ->
                    queueService.enqeue(ticket)
                }

                // todo:
                result.matches.forEach { match ->
                    println("matched (${match.player1Id}, ${match.player2Id})")
                }
                val timeEnd = System.currentTimeMillis()

                delay = max(0L, MATCHMAKING_PERIOD - (timeEnd - timeStart))
            }
        }
    }

    fun shutdown() {
        worker.interrupt()
        worker.join()
    }
}

data class MatchmakingResult(
    val matches: List<Match>,
    val ticketsLeft: List<Ticket>
)

fun makeMatches(tickets: List<Ticket>, clusterCount: Int): MatchmakingResult {
    if (tickets.isEmpty()) return MatchmakingResult(listOf(), listOf())

    val ticketsV = VectorBuffer(2, tickets.size)
    tickets.forEachIndexed { i, ticket ->
        ticketsV.seek(i)
        ticketsV[0] = ticket.skill.toDouble()
        ticketsV[1] = ticket.latency.toDouble()
    }

    if (currentThread().isInterrupted) throw InterruptedException("Matchmaking thread was interrupted")

    val labels = kmeans(clusterCount, ticketsV)
    val clusters = tickets.withIndex().groupBy(
        { (i, _) -> labels[i] },
        { (_, ticket) -> ticket }
    )

    if (currentThread().isInterrupted) throw InterruptedException("Matchmaking thread was interrupted")

    val matches = mutableListOf<Match>()
    val ticketsLeft = mutableListOf<Ticket>()
    clusters.forEach { (_, tickets) ->
        var i = 0
        while (i < tickets.size - 1) {
            matches.add(Match(tickets[i].playerId, tickets[i + 1].playerId))
            i += 2
        }
        if (i < tickets.size) {
            ticketsLeft.add(tickets[i])
        }
    }

    return MatchmakingResult(matches, ticketsLeft)
}

fun selectClusterCount(ticketsCount: Int): Int {
    CLUSTER_COUNT.forEachIndexed { i, threshold ->
        if (ticketsCount < threshold) {
            return i + 1
        }
    }
    return CLUSTER_COUNT.size
}

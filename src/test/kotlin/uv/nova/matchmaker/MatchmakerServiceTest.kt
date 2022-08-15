package uv.nova.matchmaker

import uv.nova.matchmaker.core.Match
import uv.nova.matchmaker.core.Ticket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchmakerServiceTest {

    @Test
    fun testMatchmaking() {
        val tickets = listOf(
            Ticket(1L, 2000, 100, listOf()),
            Ticket(2L, 3500, 80, listOf()),
            Ticket(3L, 1800, 200, listOf()),
            Ticket(4L, 3400, 160, listOf()),
        )

        val result = makeMatches(tickets, 2)
        assertEquals(2, result.matches.size)
        assertTrue(result.matches.any { it.between(1L, 3L) })
        assertTrue(result.matches.any { it.between(2L, 4L) })

        assertEquals(0, result.ticketsLeft.size)
    }

    @Test
    fun testTicketLeft() {
        val tickets = listOf(
            Ticket(1L, 2000, 100, listOf()),
            Ticket(2L, 3500, 80, listOf()),
            Ticket(3L, 1800, 200, listOf()),
            Ticket(4L, 3400, 160, listOf()),
            Ticket(5L, 2500, 140, listOf()),
        )

        val result = makeMatches(tickets, 2)
        assertEquals(2, result.matches.size)
        assertEquals(1, result.ticketsLeft.size)
    }


    private fun Match.between(player1Id: Long, player2Id: Long): Boolean {
        return (this.player1Id == player1Id && this.player2Id == player2Id) ||
            (this.player2Id == player1Id && this.player1Id == player2Id)
    }

}

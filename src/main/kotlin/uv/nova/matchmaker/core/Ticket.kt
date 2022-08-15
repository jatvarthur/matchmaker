package uv.nova.matchmaker.core


typealias EntityId = Long

data class Tag(val value: String)

data class Ticket(
    val playerId: EntityId,
    val skill: Int,
    val latency: Int,
    val tags: List<Tag>
)

data class Match(
    val player1Id: EntityId,
    val player2Id: EntityId
)

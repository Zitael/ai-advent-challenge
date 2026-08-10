package ru.maleks.ai_advent_challenge_app.battle

data class BattleChatRequest(
    val sessionId: String = "",
    val message: String = ""
)

data class BattleChatResponse(
    val sessionId: String,
    val answer: String,
    val model: String,
    val durationMillis: Long,
    val historyMessages: Int,
    val guards: BattleGuardSummary
)

data class BattleGuardSummary(
    val injectionBlocked: Boolean,
    val injectionPatterns: List<String>,
    val indirectArtifactsRemoved: List<String>,
    val gatewayInputFindings: List<String>,
    val gatewayOutputViolations: List<String>,
    val outputBlocked: Boolean,
    val secretLeakBlocked: Boolean = false,
    val leakedSecretHints: List<String> = emptyList()
)

data class BattleHealthResponse(
    val status: String,
    val service: String,
    val model: String,
    val layers: List<String>
)

data class BattleRedTeamAttack(
    val id: String,
    val category: String,
    val payload: String,
    val expectedBlocker: String
)

data class BattleRedTeamResult(
    val attack: BattleRedTeamAttack,
    val blocked: Boolean,
    val blockedBy: List<String>,
    val passedThrough: List<String>
)

data class BattleRedTeamReport(
    val totalAttacks: Int,
    val blockedCount: Int,
    val passedCount: Int,
    val results: List<BattleRedTeamResult>
)

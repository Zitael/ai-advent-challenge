package ru.maleks.ai_advent_challenge_app.classification

class RedundancyChecker(
    private val gateway: TicketClassificationGateway,
    private val voteCount: Int = 3
) {
    suspend fun check(ticketText: String): RedundancyCheckResult {
        val votes = mutableListOf<String>()
        val metrics = mutableListOf<LlmCallMetrics>()

        repeat(voteCount) {
            val (rawAnswer, callMetrics) = gateway.classify(
                ticketText = ticketText,
                temperature = 0.5
            )
            metrics += callMetrics
            votes += ClassificationAnswerParser.parseCategory(rawAnswer).orEmpty()
        }

        val nonEmptyVotes = votes.filter { it.isNotBlank() }
        val grouped = nonEmptyVotes.groupingBy { it }.eachCount()
        val winner = grouped.maxByOrNull { it.value }
        val consensus = winner?.key
        val agreementRatio = winner?.value?.toDouble()?.div(voteCount) ?: 0.0
        val passed = (winner?.value ?: 0) >= 2

        return RedundancyCheckResult(
            passed = passed,
            consensusCategory = consensus,
            votes = votes,
            agreementRatio = agreementRatio,
            details = "Votes=$votes, consensus=$consensus, agreement=${"%.0f".format(agreementRatio * 100)}%",
            metrics = metrics.merge()
        )
    }
}

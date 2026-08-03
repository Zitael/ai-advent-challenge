package ru.maleks.ai_advent_challenge_app.classification

import ru.maleks.ai_advent_challenge_app.dataset.TicketCategory
import ru.maleks.ai_advent_challenge_app.llm.LlmClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import kotlin.system.measureTimeMillis

class SelfCheckVerifier(
    private val llmClient: LlmClient
) {
    suspend fun verify(
        ticketText: String,
        proposedCategory: String
    ): SelfCheckResult {
        var rawAnswer = ""
        var metrics = LlmCallMetrics(
            latencyMs = 0,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            cost = null
        )

        val latencyMs = measureTimeMillis {
            val result = llmClient.complete(
                messages = listOf(
                    OpenRouterMessage(
                        role = "system",
                        content = """
                            You verify support ticket classifications.
                            Allowed categories: ${TicketCategory.labels.joinToString(", ")}.
                            Reply with exactly one word.
                            Reply YES if the proposed category is correct.
                            Otherwise reply with the correct category word only.
                        """.trimIndent()
                    ),
                    OpenRouterMessage(
                        role = "user",
                        content = """
                            Ticket: ${ticketText.trim()}
                            Proposed category: $proposedCategory
                        """.trimIndent()
                    )
                ),
                temperature = 0.1
            )
            rawAnswer = result.answer
            metrics = result.usage.toMetrics()
        }

        val normalized = ClassificationAnswerParser.normalize(rawAnswer)
        val verifiedCategory = when {
            normalized == "yes" -> proposedCategory
            else -> ClassificationAnswerParser.parseCategory(normalized)
        }
        val passed = verifiedCategory == proposedCategory

        return SelfCheckResult(
            passed = passed,
            verifiedCategory = verifiedCategory,
            rawAnswer = rawAnswer,
            details = if (passed) {
                "Self-check confirmed $proposedCategory"
            } else {
                "Self-check corrected to $verifiedCategory (was $proposedCategory)"
            },
            metrics = metrics.copy(latencyMs = latencyMs)
        )
    }
}

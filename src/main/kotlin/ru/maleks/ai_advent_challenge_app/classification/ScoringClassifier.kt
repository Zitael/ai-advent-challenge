package ru.maleks.ai_advent_challenge_app.classification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ru.maleks.ai_advent_challenge_app.dataset.TicketCategory
import ru.maleks.ai_advent_challenge_app.llm.LlmClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import kotlin.system.measureTimeMillis

class ScoringClassifier(
    private val llmClient: LlmClient
) {
    private val mapper = jacksonObjectMapper()

    suspend fun classify(ticketText: String): ScoringResult {
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
                            Classify the support ticket and estimate confidence.
                            Allowed categories: ${TicketCategory.labels.joinToString(", ")}.
                            Reply with JSON only:
                            {"category":"...","confidence":0.0,"status":"OK|UNSURE|FAIL"}
                            Rules:
                            - confidence is between 0 and 1
                            - status OK if confidence >= 0.8
                            - status UNSURE if confidence is 0.5..0.79
                            - status FAIL if confidence < 0.5
                        """.trimIndent()
                    ),
                    OpenRouterMessage(
                        role = "user",
                        content = "Ticket: ${ticketText.trim()}"
                    )
                ),
                temperature = 0.2
            )
            rawAnswer = result.answer
            metrics = result.usage.toMetrics()
        }

        return parseResponse(rawAnswer, metrics.copy(latencyMs = latencyMs))
    }

    private fun parseResponse(rawAnswer: String, metrics: LlmCallMetrics): ScoringResult {
        val jsonPayload = extractJson(rawAnswer)
            ?: return ScoringResult(
                passed = false,
                category = null,
                confidence = null,
                status = ConfidenceStatus.FAIL,
                rawAnswer = rawAnswer,
                details = "Scoring response is not valid JSON",
                metrics = metrics
            )

        val parsed = runCatching {
            mapper.readValue<ScoringPayload>(jsonPayload)
        }.getOrElse { exception ->
            return ScoringResult(
                passed = false,
                category = null,
                confidence = null,
                status = ConfidenceStatus.FAIL,
                rawAnswer = rawAnswer,
                details = "Failed to parse scoring JSON: ${exception.message}",
                metrics = metrics
            )
        }

        val category = ClassificationAnswerParser.parseCategory(parsed.category.orEmpty())
        val confidence = parsed.confidence?.coerceIn(0.0, 1.0)
        val status = parsed.status?.let { parseStatus(it) }
            ?: confidence?.let { scoreToStatus(it) }

        val passed = category != null &&
            confidence != null &&
            status != null &&
            status != ConfidenceStatus.FAIL

        return ScoringResult(
            passed = passed,
            category = category,
            confidence = confidence,
            status = status,
            rawAnswer = rawAnswer,
            details = "Scoring category=$category, confidence=$confidence, status=$status",
            metrics = metrics
        )
    }

    private fun extractJson(rawAnswer: String): String? {
        val trimmed = rawAnswer.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1)
        }

        return null
    }

    private fun parseStatus(rawStatus: String): ConfidenceStatus? =
        when (rawStatus.trim().uppercase()) {
            "OK" -> ConfidenceStatus.OK
            "UNSURE" -> ConfidenceStatus.UNSURE
            "FAIL" -> ConfidenceStatus.FAIL
            else -> null
        }

    private fun scoreToStatus(confidence: Double): ConfidenceStatus =
        when {
            confidence >= 0.8 -> ConfidenceStatus.OK
            confidence >= 0.5 -> ConfidenceStatus.UNSURE
            else -> ConfidenceStatus.FAIL
        }

    private data class ScoringPayload(
        val category: String? = null,
        val confidence: Double? = null,
        val status: String? = null
    )
}

package ru.maleks.ai_advent_challenge_app.routing

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ru.maleks.ai_advent_challenge_app.classification.ConfidenceStatus
import ru.maleks.ai_advent_challenge_app.classification.LlmCallMetrics
import ru.maleks.ai_advent_challenge_app.classification.ScoringResult

class RoutingHeuristicsTest {

    private val heuristics = RoutingHeuristics()

    @Test
    fun `does not escalate when confidence and status are strong`() {
        val evaluation = heuristics.evaluate(
            ticketText = "Списали дважды за PRO подписку, прошу вернуть деньги.",
            scoring = scoring(
                category = "billing",
                confidence = 0.92,
                status = ConfidenceStatus.OK
            )
        )

        assertFalse(evaluation.shouldEscalate)
        assertTrue(evaluation.checks.all { it.passed })
    }

    @Test
    fun `escalates on multi domain ticket`() {
        val evaluation = heuristics.evaluate(
            ticketText = "Оплатил тариф, но доступ не появился и войти в аккаунт тоже не могу.",
            scoring = scoring(
                category = "billing",
                confidence = 0.92,
                status = ConfidenceStatus.OK
            )
        )

        assertTrue(evaluation.shouldEscalate)
        assertFalse(evaluation.checks.first { it.name == "input_complexity" }.passed)
    }

    @Test
    fun `escalates on low confidence`() {
        val evaluation = heuristics.evaluate(
            ticketText = "Списали дважды за PRO подписку.",
            scoring(
                category = "billing",
                confidence = 0.55,
                status = ConfidenceStatus.UNSURE
            )
        )

        assertTrue(evaluation.shouldEscalate)
        assertFalse(evaluation.checks.first { it.name == "confidence_score" }.passed)
    }

    @Test
    fun `escalates on unsure status`() {
        val evaluation = heuristics.evaluate(
            ticketText = "API возвращает 500.",
            scoring(
                category = "technical",
                confidence = 0.9,
                status = ConfidenceStatus.UNSURE
            )
        )

        assertTrue(evaluation.shouldEscalate)
        assertFalse(evaluation.checks.first { it.name == "unsure_rule" }.passed)
    }

    @Test
    fun `escalates on invalid category length`() {
        val evaluation = heuristics.evaluate(
            ticketText = "Платёж не прошёл.",
            scoring(
                category = "bad",
                confidence = 0.95,
                status = ConfidenceStatus.OK
            )
        )

        assertTrue(evaluation.shouldEscalate)
        assertFalse(evaluation.checks.first { it.name == "constraint" }.passed)
    }

    private fun scoring(
        category: String,
        confidence: Double,
        status: ConfidenceStatus
    ): ScoringResult =
        ScoringResult(
            passed = true,
            category = category,
            confidence = confidence,
            status = status,
            rawAnswer = """{"category":"$category","confidence":$confidence,"status":"${status.name}"}""",
            details = "test",
            metrics = LlmCallMetrics(0, 0, 0, 0, null)
        )
}

package ru.maleks.ai_advent_challenge_app.micromodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TicketKeywordMicroClassifierTest {

    private val classifier = TicketKeywordMicroClassifier()

    @Test
    fun `classifies clear billing ticket as OK`() {
        val result = classifier.classify(
            "Списали дважды за PRO подписку, прошу вернуть деньги."
        )

        assertEquals(MicroModelStatus.OK, result.status)
        assertEquals("billing", result.category)
        assertTrue(result.confidence >= 0.65)
    }

    @Test
    fun `returns UNSURE for mixed domain ticket`() {
        val result = classifier.classify(
            "Оплатил тариф, но доступ не появился и войти в аккаунт не могу."
        )

        assertEquals(MicroModelStatus.UNSURE, result.status)
    }

    @Test
    fun `returns UNSURE when no signals found`() {
        val result = classifier.classify("help pls")

        assertEquals(MicroModelStatus.UNSURE, result.status)
        assertEquals(0.0, result.confidence)
    }
}

class MicroModelDecisionPolicyTest {

    private val policy = MicroModelDecisionPolicy(confidenceThreshold = 0.65)

    @Test
    fun `accepts confident micro result`() {
        val decision = policy.evaluate(
            MicroClassificationResult(
                category = "billing",
                confidence = 0.9,
                status = MicroModelStatus.OK,
                matchedSignals = listOf("оплат", "refund"),
                latencyMs = 1
            )
        )

        assertFalse(decision.useFallback)
    }

    @Test
    fun `falls back on unsure status`() {
        val decision = policy.evaluate(
            MicroClassificationResult(
                category = null,
                confidence = 0.4,
                status = MicroModelStatus.UNSURE,
                matchedSignals = emptyList(),
                latencyMs = 1
            )
        )

        assertTrue(decision.useFallback)
    }
}

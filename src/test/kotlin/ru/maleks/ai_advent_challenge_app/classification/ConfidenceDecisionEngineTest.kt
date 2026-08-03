package ru.maleks.ai_advent_challenge_app.classification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfidenceDecisionEngineTest {

    private val engine = ConfidenceDecisionEngine()

    @Test
    fun `accepts when all checks pass`() {
        val decision = engine.decide(
            ConfidenceDecisionEngine.DecisionInput(
                scoring = scoring(status = ConfidenceStatus.OK, category = "billing"),
                constraint = constraint(passed = true, category = "billing"),
                redundancy = redundancy(passed = true, category = "billing"),
                selfCheck = selfCheck(passed = true, category = "billing")
            )
        )

        assertEquals(ConfidenceStatus.OK, decision.status)
        assertEquals("billing", decision.acceptedCategory)
        assertFalse(decision.rejected)
    }

    @Test
    fun `rejects invalid format`() {
        val decision = engine.decide(
            ConfidenceDecisionEngine.DecisionInput(
                scoring = scoring(status = ConfidenceStatus.UNSURE, category = "billing"),
                constraint = constraint(passed = false, category = null),
                redundancy = redundancy(passed = true, category = "billing"),
                selfCheck = selfCheck(passed = true, category = "billing")
            )
        )

        assertEquals(ConfidenceStatus.REJECTED, decision.status)
        assertNull(decision.acceptedCategory)
        assertTrue(decision.rejected)
    }

    @Test
    fun `marks unsure when scoring and redundancy disagree`() {
        val decision = engine.decide(
            ConfidenceDecisionEngine.DecisionInput(
                scoring = scoring(status = ConfidenceStatus.OK, category = "billing"),
                constraint = constraint(passed = true, category = "billing"),
                redundancy = redundancy(passed = true, category = "technical"),
                selfCheck = selfCheck(passed = true, category = "billing")
            )
        )

        assertEquals(ConfidenceStatus.UNSURE, decision.status)
        assertNull(decision.acceptedCategory)
        assertTrue(decision.rejected)
    }

    @Test
    fun `marks unsure when redundancy disagrees`() {
        val decision = engine.decide(
            ConfidenceDecisionEngine.DecisionInput(
                scoring = scoring(status = ConfidenceStatus.OK, category = "billing"),
                constraint = constraint(passed = true, category = "billing"),
                redundancy = redundancy(passed = false, category = "account"),
                selfCheck = selfCheck(passed = false, category = "account")
            )
        )

        assertEquals(ConfidenceStatus.UNSURE, decision.status)
        assertNull(decision.acceptedCategory)
        assertTrue(decision.rejected)
    }

    private fun scoring(status: ConfidenceStatus, category: String): ScoringResult =
        ScoringResult(
            passed = true,
            category = category,
            confidence = 0.9,
            status = status,
            rawAnswer = """{"category":"$category","confidence":0.9,"status":"OK"}""",
            details = "test",
            metrics = metrics()
        )

    private fun constraint(passed: Boolean, category: String?): ConstraintCheckResult =
        ConstraintCheckResult(
            passed = passed,
            category = category,
            details = "test"
        )

    private fun redundancy(passed: Boolean, category: String): RedundancyCheckResult =
        RedundancyCheckResult(
            passed = passed,
            consensusCategory = category,
            votes = listOf(category, category, "technical"),
            agreementRatio = if (passed) 0.67 else 0.33,
            details = "test",
            metrics = metrics()
        )

    private fun selfCheck(passed: Boolean, category: String): SelfCheckResult =
        SelfCheckResult(
            passed = passed,
            verifiedCategory = category,
            rawAnswer = if (passed) "yes" else category,
            details = "test",
            metrics = metrics()
        )

    private fun metrics(): LlmCallMetrics =
        LlmCallMetrics(0, 0, 0, 0, null)
}

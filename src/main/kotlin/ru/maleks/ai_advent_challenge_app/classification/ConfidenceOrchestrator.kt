package ru.maleks.ai_advent_challenge_app.classification

class ConfidenceOrchestrator(
    private val scoringClassifier: ScoringClassifier,
    private val constraintValidator: ConstraintValidator,
    private val redundancyChecker: RedundancyChecker,
    private val selfCheckVerifier: SelfCheckVerifier,
    private val decisionEngine: ConfidenceDecisionEngine = ConfidenceDecisionEngine()
) {

    suspend fun classify(testCase: ClassificationTestCase): ClassificationOutcome {
        val firstPass = runPipeline(testCase.ticketText)
        if (firstPass.decision.status == ConfidenceStatus.OK) {
            return firstPass.toOutcome(
                testCase = testCase,
                retried = false,
                inferenceCalls = firstPass.inferenceCalls
            )
        }

        val retryPass = runPipeline(testCase.ticketText)
        val combinedMetrics = firstPass.metrics + retryPass.metrics

        return ClassificationOutcome(
            testCaseId = testCase.id,
            kind = testCase.kind,
            acceptedCategory = retryPass.decision.acceptedCategory,
            finalStatus = retryPass.decision.status,
            expectedCategory = testCase.expectedCategory,
            correct = isCorrect(retryPass.decision.acceptedCategory, testCase.expectedCategory),
            candidateCategory = retryPass.decision.candidateCategory,
            approaches = retryPass.decision.approaches,
            inferenceCalls = firstPass.inferenceCalls + retryPass.inferenceCalls,
            rejected = retryPass.decision.rejected,
            retried = true,
            totalLatencyMs = combinedMetrics.latencyMs,
            totalTokens = combinedMetrics.totalTokens,
            totalCost = combinedMetrics.cost
        )
    }

    private suspend fun runPipeline(ticketText: String): PipelinePass {
        val scoring = scoringClassifier.classify(ticketText)
        val constraint = constraintValidator.validate(
            scoring.category ?: scoring.rawAnswer
        )

        val candidateForChecks = constraint.category
            ?: scoring.category
            ?: ClassificationAnswerParser.parseCategory(scoring.rawAnswer)

        val redundancy = redundancyChecker.check(ticketText)
        val selfCheck = if (candidateForChecks != null) {
            selfCheckVerifier.verify(ticketText, candidateForChecks)
        } else {
            SelfCheckResult(
                passed = false,
                verifiedCategory = null,
                rawAnswer = "",
                details = "Self-check skipped: no candidate category",
                metrics = LlmCallMetrics(0, 0, 0, 0, null)
            )
        }

        val decision = decisionEngine.decide(
            ConfidenceDecisionEngine.DecisionInput(
                scoring = scoring,
                constraint = constraint,
                redundancy = redundancy,
                selfCheck = selfCheck
            )
        )

        val metrics = scoring.metrics +
            redundancy.metrics +
            selfCheck.metrics

        return PipelinePass(
            decision = decision,
            metrics = metrics,
            inferenceCalls = 1 + redundancy.votes.size + 1
        )
    }

    private fun PipelinePass.toOutcome(
        testCase: ClassificationTestCase,
        retried: Boolean,
        inferenceCalls: Int
    ): ClassificationOutcome =
        ClassificationOutcome(
            testCaseId = testCase.id,
            kind = testCase.kind,
            acceptedCategory = decision.acceptedCategory,
            finalStatus = decision.status,
            expectedCategory = testCase.expectedCategory,
            correct = isCorrect(decision.acceptedCategory, testCase.expectedCategory),
            candidateCategory = decision.candidateCategory,
            approaches = decision.approaches,
            inferenceCalls = inferenceCalls,
            rejected = decision.rejected,
            retried = retried,
            totalLatencyMs = metrics.latencyMs,
            totalTokens = metrics.totalTokens,
            totalCost = metrics.cost
        )

    private fun isCorrect(acceptedCategory: String?, expectedCategory: String?): Boolean? {
        if (expectedCategory == null) {
            return null
        }
        if (acceptedCategory == null) {
            return false
        }
        return acceptedCategory == expectedCategory
    }

    private data class PipelinePass(
        val decision: ConfidenceDecisionEngine.Decision,
        val metrics: LlmCallMetrics,
        val inferenceCalls: Int
    )
}

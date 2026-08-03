package ru.maleks.ai_advent_challenge_app.micromodel

import ru.maleks.ai_advent_challenge_app.classification.ClassificationAnswerParser
import ru.maleks.ai_advent_challenge_app.classification.TicketClassificationGateway

class MicroFirstPipeline(
    private val microClassifier: TicketKeywordMicroClassifier,
    private val decisionPolicy: MicroModelDecisionPolicy,
    private val fallbackGateway: TicketClassificationGateway
) {

    suspend fun classify(testCase: MicromodelTestCase): MicromodelOutcome {
        val microResult = microClassifier.classify(testCase.ticketText)
        val decision = decisionPolicy.evaluate(microResult)

        if (!decision.useFallback) {
            return MicromodelOutcome(
                testCaseId = testCase.id,
                kind = testCase.kind,
                handledByMicro = true,
                fallbackUsed = false,
                microStatus = microResult.status,
                microConfidence = microResult.confidence,
                microCategory = microResult.category,
                finalCategory = microResult.category,
                expectedCategory = testCase.expectedCategory,
                correct = isCorrect(microResult.category, testCase.expectedCategory),
                fallbackReasons = emptyList(),
                microLatencyMs = microResult.latencyMs,
                fallbackLatencyMs = 0,
                totalLatencyMs = microResult.latencyMs,
                fallbackTokens = 0,
                fallbackCost = null
            )
        }

        val (rawAnswer, fallbackMetrics) = fallbackGateway.classify(
            ticketText = testCase.ticketText,
            temperature = 0.2
        )
        val finalCategory = ClassificationAnswerParser.parseCategory(rawAnswer)

        return MicromodelOutcome(
            testCaseId = testCase.id,
            kind = testCase.kind,
            handledByMicro = false,
            fallbackUsed = true,
            microStatus = microResult.status,
            microConfidence = microResult.confidence,
            microCategory = microResult.category,
            finalCategory = finalCategory,
            expectedCategory = testCase.expectedCategory,
            correct = isCorrect(finalCategory, testCase.expectedCategory),
            fallbackReasons = decision.reasons,
            microLatencyMs = microResult.latencyMs,
            fallbackLatencyMs = fallbackMetrics.latencyMs,
            totalLatencyMs = microResult.latencyMs + fallbackMetrics.latencyMs,
            fallbackTokens = fallbackMetrics.totalTokens,
            fallbackCost = fallbackMetrics.cost
        )
    }

    private fun isCorrect(finalCategory: String?, expectedCategory: String?): Boolean? {
        if (expectedCategory == null) {
            return null
        }
        if (finalCategory == null) {
            return false
        }
        return finalCategory == expectedCategory
    }
}

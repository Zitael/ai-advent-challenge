package ru.maleks.ai_advent_challenge_app.routing

import ru.maleks.ai_advent_challenge_app.classification.ClassificationAnswerParser
import ru.maleks.ai_advent_challenge_app.classification.ScoringClassifier
import ru.maleks.ai_advent_challenge_app.classification.ScoringResult
import ru.maleks.ai_advent_challenge_app.classification.TicketClassificationGateway

class ModelRouter(
    private val cheapScoringClassifier: ScoringClassifier,
    private val strongGateway: TicketClassificationGateway,
    private val heuristics: RoutingHeuristics = RoutingHeuristics()
) {

    suspend fun route(testCase: RoutingTestCase): RoutingOutcome {
        val cheapScoring = cheapScoringClassifier.classify(testCase.ticketText)
        val evaluation = heuristics.evaluate(testCase.ticketText, cheapScoring)

        if (!evaluation.shouldEscalate) {
            return cheapOutcome(testCase, cheapScoring, evaluation)
        }

        val (strongRaw, strongMetrics) = strongGateway.classify(
            ticketText = testCase.ticketText,
            temperature = 0.2
        )
        val strongCategory = ClassificationAnswerParser.parseCategory(strongRaw)

        return RoutingOutcome(
            testCaseId = testCase.id,
            kind = testCase.kind,
            routedModel = RoutedModel.STRONG,
            escalated = true,
            escalationReasons = evaluation.checks.filter { !it.passed }.map { it.details },
            acceptedCategory = strongCategory,
            expectedCategory = testCase.expectedCategory,
            correct = isCorrect(strongCategory, testCase.expectedCategory),
            cheapConfidence = cheapScoring.confidence,
            cheapStatus = cheapScoring.status?.name,
            heuristics = evaluation.checks,
            cheapLatencyMs = cheapScoring.metrics.latencyMs,
            strongLatencyMs = strongMetrics.latencyMs,
            totalLatencyMs = cheapScoring.metrics.latencyMs + strongMetrics.latencyMs,
            cheapTokens = cheapScoring.metrics.totalTokens,
            strongTokens = strongMetrics.totalTokens,
            totalTokens = cheapScoring.metrics.totalTokens + strongMetrics.totalTokens,
            cheapCost = cheapScoring.metrics.cost,
            strongCost = strongMetrics.cost,
            totalCost = listOfNotNull(cheapScoring.metrics.cost, strongMetrics.cost)
                .takeIf { it.isNotEmpty() }
                ?.sum()
        )
    }

    private fun cheapOutcome(
        testCase: RoutingTestCase,
        cheapScoring: ScoringResult,
        evaluation: HeuristicEvaluation
    ): RoutingOutcome {
        val category = cheapScoring.category

        return RoutingOutcome(
            testCaseId = testCase.id,
            kind = testCase.kind,
            routedModel = RoutedModel.CHEAP,
            escalated = false,
            escalationReasons = emptyList(),
            acceptedCategory = category,
            expectedCategory = testCase.expectedCategory,
            correct = isCorrect(category, testCase.expectedCategory),
            cheapConfidence = cheapScoring.confidence,
            cheapStatus = cheapScoring.status?.name,
            heuristics = evaluation.checks,
            cheapLatencyMs = cheapScoring.metrics.latencyMs,
            strongLatencyMs = 0,
            totalLatencyMs = cheapScoring.metrics.latencyMs,
            cheapTokens = cheapScoring.metrics.totalTokens,
            strongTokens = 0,
            totalTokens = cheapScoring.metrics.totalTokens,
            cheapCost = cheapScoring.metrics.cost,
            strongCost = null,
            totalCost = cheapScoring.metrics.cost
        )
    }

    private fun isCorrect(acceptedCategory: String?, expectedCategory: String?): Boolean? {
        if (expectedCategory == null) {
            return null
        }
        if (acceptedCategory == null) {
            return false
        }
        return acceptedCategory == expectedCategory
    }
}

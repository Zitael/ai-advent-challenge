package ru.maleks.ai_advent_challenge_app.routing

import ru.maleks.ai_advent_challenge_app.classification.ClassificationAnswerParser
import ru.maleks.ai_advent_challenge_app.classification.ConfidenceStatus
import ru.maleks.ai_advent_challenge_app.classification.ConstraintValidator
import ru.maleks.ai_advent_challenge_app.classification.ScoringResult

class RoutingHeuristics(
    private val constraintValidator: ConstraintValidator = ConstraintValidator(),
    private val confidenceThreshold: Double = 0.8,
    private val minCategoryLength: Int = 4,
    private val maxCategoryLength: Int = 20
) {

    fun evaluate(ticketText: String, scoring: ScoringResult): HeuristicEvaluation {
        val checks = listOf(
            confidenceCheck(scoring),
            answerLengthCheck(scoring),
            unsureRuleCheck(scoring),
            constraintCheck(scoring),
            inputComplexityCheck(ticketText)
        )

        return HeuristicEvaluation(
            shouldEscalate = checks.any { !it.passed },
            checks = checks
        )
    }

    private fun confidenceCheck(scoring: ScoringResult): HeuristicCheck {
        val confidence = scoring.confidence
        val passed = confidence != null && confidence >= confidenceThreshold

        return HeuristicCheck(
            name = "confidence_score",
            passed = passed,
            details = when {
                confidence == null -> "Confidence is missing"
                passed -> "Confidence ${"%.2f".format(confidence)} >= $confidenceThreshold"
                else -> "Confidence ${"%.2f".format(confidence)} < $confidenceThreshold"
            }
        )
    }

    private fun answerLengthCheck(scoring: ScoringResult): HeuristicCheck {
        val category = scoring.category
        val normalized = category?.let { ClassificationAnswerParser.normalize(it) }.orEmpty()
        val passed = normalized.isNotBlank() &&
            normalized.length in minCategoryLength..maxCategoryLength &&
            normalized.split(Regex("\\s+")).size == 1

        return HeuristicCheck(
            name = "answer_length",
            passed = passed,
            details = when {
                category == null -> "Category is missing"
                passed -> "Category length ${normalized.length} is within $minCategoryLength..$maxCategoryLength"
                else -> "Category '$normalized' has invalid length or format"
            }
        )
    }

    private fun unsureRuleCheck(scoring: ScoringResult): HeuristicCheck {
        val passed = scoring.status == ConfidenceStatus.OK

        return HeuristicCheck(
            name = "unsure_rule",
            passed = passed,
            details = when (scoring.status) {
                ConfidenceStatus.OK -> "Scoring status is OK"
                ConfidenceStatus.UNSURE -> "Scoring status is UNSURE — escalate"
                ConfidenceStatus.FAIL -> "Scoring status is FAIL — escalate"
                ConfidenceStatus.REJECTED -> "Scoring status is REJECTED — escalate"
                null -> "Scoring status is missing — escalate"
            }
        )
    }

    private fun constraintCheck(scoring: ScoringResult): HeuristicCheck {
        val constraint = constraintValidator.validate(
            scoring.category ?: scoring.rawAnswer
        )

        return HeuristicCheck(
            name = "constraint",
            passed = constraint.passed,
            details = constraint.details
        )
    }

    private fun inputComplexityCheck(ticketText: String): HeuristicCheck {
        val normalized = ticketText.lowercase()
        val matchedDomains = domainKeywordGroups.count { keywords ->
            keywords.any { keyword -> normalized.contains(keyword) }
        }
        val noisy = looksNoisy(normalized)

        val passed = matchedDomains <= 1 && !noisy

        return HeuristicCheck(
            name = "input_complexity",
            passed = passed,
            details = when {
                noisy -> "Noisy input detected — escalate"
                matchedDomains > 1 -> "Multiple domains in ticket ($matchedDomains) — escalate"
                else -> "Input complexity is low"
            }
        )
    }

    private fun looksNoisy(text: String): Boolean {
        if (text.contains("???") || text.contains("asdf")) {
            return true
        }

        val noiseChars = text.count { character ->
            !character.isLetter() &&
                !character.isWhitespace() &&
                !character.isDigit() &&
                character !in ALLOWED_PUNCTUATION
        }

        return noiseChars >= 6 && noiseChars.toDouble() / text.length.coerceAtLeast(1) > 0.2
    }

    private companion object {
        val ALLOWED_PUNCTUATION = setOf('/', '-', '_', '.', ',')
        val domainKeywordGroups = listOf(
            listOf("оплат", "billing", "invoice", "плат", "refund", "сч", "тариф", "payment"),
            listOf("аккаунт", "account", "login", "sso", "password", "вход", "профил", "workspace"),
            listOf("api", "timeout", "502", "500", "error", "deploy", "mcp", "rag", "gradle", "technical"),
            listOf("webhook", "feature", "dark mode", "sla", "dashboard", "auditor", "telegram")
        )
    }
}

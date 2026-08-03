package ru.maleks.ai_advent_challenge_app.classification

class ConstraintValidator {

    fun validate(rawAnswer: String): ConstraintCheckResult {
        val normalized = ClassificationAnswerParser.normalize(rawAnswer)
        val category = ClassificationAnswerParser.parseCategory(normalized)

        if (normalized.isBlank()) {
            return ConstraintCheckResult(
                passed = false,
                category = null,
                details = "Empty model answer"
            )
        }

        if (normalized.contains('\n')) {
            return ConstraintCheckResult(
                passed = false,
                category = category,
                details = "Answer contains multiple lines"
            )
        }

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size != 1) {
            return ConstraintCheckResult(
                passed = false,
                category = category,
                details = "Expected exactly one token, got ${tokens.size}"
            )
        }

        if (category == null) {
            return ConstraintCheckResult(
                passed = false,
                category = null,
                details = "Category '$normalized' is not in allowed set"
            )
        }

        return ConstraintCheckResult(
            passed = true,
            category = category,
            details = "Valid category token: $category"
        )
    }
}

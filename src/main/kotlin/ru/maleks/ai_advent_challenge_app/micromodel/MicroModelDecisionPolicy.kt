package ru.maleks.ai_advent_challenge_app.micromodel

import ru.maleks.ai_advent_challenge_app.classification.ConstraintValidator
import ru.maleks.ai_advent_challenge_app.dataset.TicketCategory

class MicroModelDecisionPolicy(
    private val constraintValidator: ConstraintValidator = ConstraintValidator(),
    private val confidenceThreshold: Double = 0.65
) {

    data class Decision(
        val useFallback: Boolean,
        val reasons: List<String>
    )

    fun evaluate(microResult: MicroClassificationResult): Decision {
        val reasons = mutableListOf<String>()

        if (microResult.status == MicroModelStatus.UNSURE) {
            reasons += "Micro-model status is UNSURE"
        }

        if (microResult.confidence < confidenceThreshold) {
            reasons += "Confidence ${"%.2f".format(microResult.confidence)} < $confidenceThreshold"
        }

        val category = microResult.category
        if (category.isNullOrBlank()) {
            reasons += "Micro-model category is missing"
        } else {
            val constraint = constraintValidator.validate(category)
            if (!constraint.passed) {
                reasons += "Invalid micro-model format: ${constraint.details}"
            }
            if (TicketCategory.fromLabel(category) == null) {
                reasons += "Category '$category' is not allowed"
            }
        }

        return Decision(
            useFallback = reasons.isNotEmpty(),
            reasons = reasons
        )
    }
}

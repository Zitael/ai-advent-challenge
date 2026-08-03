package ru.maleks.ai_advent_challenge_app.classification

class ConfidenceDecisionEngine {

    data class DecisionInput(
        val scoring: ScoringResult,
        val constraint: ConstraintCheckResult,
        val redundancy: RedundancyCheckResult,
        val selfCheck: SelfCheckResult
    )

    data class Decision(
        val status: ConfidenceStatus,
        val candidateCategory: String?,
        val acceptedCategory: String?,
        val rejected: Boolean,
        val approaches: List<ApproachOutcome>
    )

    fun decide(input: DecisionInput): Decision {
        val candidateCategory = selectCandidate(
            scoringCategory = input.scoring.category,
            constraintCategory = input.constraint.category,
            redundancyCategory = input.redundancy.consensusCategory,
            selfCheckCategory = input.selfCheck.verifiedCategory
        )

        val categoriesAligned = categoriesAligned(input, candidateCategory)

        val approaches = listOf(
            ApproachOutcome(
                name = "scoring",
                passed = input.scoring.passed && input.scoring.status != ConfidenceStatus.FAIL,
                details = input.scoring.details
            ),
            ApproachOutcome(
                name = "constraint",
                passed = input.constraint.passed,
                details = input.constraint.details
            ),
            ApproachOutcome(
                name = "redundancy",
                passed = input.redundancy.passed &&
                    input.redundancy.consensusCategory == candidateCategory,
                details = input.redundancy.details
            ),
            ApproachOutcome(
                name = "self_check",
                passed = input.selfCheck.passed &&
                    input.selfCheck.verifiedCategory == candidateCategory,
                details = input.selfCheck.details
            )
        )

        if (!input.constraint.passed) {
            return Decision(
                status = ConfidenceStatus.REJECTED,
                candidateCategory = candidateCategory,
                acceptedCategory = null,
                rejected = true,
                approaches = approaches
            )
        }

        if (input.scoring.status == ConfidenceStatus.FAIL) {
            return Decision(
                status = ConfidenceStatus.FAIL,
                candidateCategory = candidateCategory,
                acceptedCategory = null,
                rejected = true,
                approaches = approaches
            )
        }

        val allPassed = approaches.all { it.passed } &&
            input.scoring.status == ConfidenceStatus.OK &&
            categoriesAligned

        if (allPassed && candidateCategory != null) {
            return Decision(
                status = ConfidenceStatus.OK,
                candidateCategory = candidateCategory,
                acceptedCategory = candidateCategory,
                rejected = false,
                approaches = approaches
            )
        }

        val unsure = !categoriesAligned ||
            input.scoring.status == ConfidenceStatus.UNSURE ||
            !input.redundancy.passed ||
            !input.selfCheck.passed

        return Decision(
            status = if (unsure) ConfidenceStatus.UNSURE else ConfidenceStatus.FAIL,
            candidateCategory = candidateCategory,
            acceptedCategory = null,
            rejected = true,
            approaches = approaches
        )
    }

    private fun selectCandidate(
        scoringCategory: String?,
        constraintCategory: String?,
        redundancyCategory: String?,
        selfCheckCategory: String?
    ): String? =
        listOfNotNull(scoringCategory, redundancyCategory, selfCheckCategory, constraintCategory)
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    private fun categoriesAligned(input: DecisionInput, candidateCategory: String?): Boolean {
        if (candidateCategory == null) {
            return false
        }

        return input.scoring.category == candidateCategory &&
            input.redundancy.consensusCategory == candidateCategory &&
            input.selfCheck.verifiedCategory == candidateCategory &&
            input.constraint.category == candidateCategory
    }
}

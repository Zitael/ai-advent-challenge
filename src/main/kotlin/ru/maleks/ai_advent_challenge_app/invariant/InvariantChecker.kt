package ru.maleks.ai_advent_challenge_app.invariant

class InvariantChecker(
    private val storage: InvariantStorage
) {
    private var invariants: MutableList<Invariant> = storage.load()

    fun getAll(): List<Invariant> = invariants

    fun add(invariant: Invariant) {
        invariants.removeIf { it.id == invariant.id }
        invariants.add(invariant)
        storage.save(invariants)
    }

    fun clear() {
        invariants = mutableListOf()
        storage.save(invariants)
    }

    fun check(text: String): List<InvariantViolation> {
        val normalizedText = text.lowercase()

        return invariants.mapNotNull { invariant ->
            val matched = invariant.forbiddenKeywords
                .filter { keyword -> normalizedText.contains(keyword.lowercase()) }

            if (matched.isEmpty()) {
                null
            } else {
                InvariantViolation(
                    invariantId = invariant.id,
                    invariantDescription = invariant.description,
                    matchedKeywords = matched
                )
            }
        }
    }

    fun formatForPrompt(): String {
        if (invariants.isEmpty()) {
            return "No invariants configured."
        }

        return invariants.joinToString("\n") { invariant ->
            """
            - id: ${invariant.id}
              rule: ${invariant.description}
              forbidden keywords: ${invariant.forbiddenKeywords.joinToString(", ").ifBlank { "none" }}
            """.trimIndent()
        }
    }

    fun printInvariants() {
        println()
        println("========== INVARIANTS ==========")

        if (invariants.isEmpty()) {
            println("empty")
        } else {
            invariants.forEach { invariant ->
                println("- id: ${invariant.id}")
                println("  description: ${invariant.description}")
                println("  forbidden keywords: ${invariant.forbiddenKeywords.joinToString(", ")}")
            }
        }

        println("================================")
        println()
    }
}
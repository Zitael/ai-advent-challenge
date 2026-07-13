package ru.maleks.ai_advent_challenge_app.rag.chat

class RagTaskMemoryUpdater {

    fun update(
        userInput: String,
        memory: RagTaskMemory
    ) {
        val text = userInput.trim()
        val lower = text.lowercase()

        when {
            lower.startsWith("goal:") ||
                    lower.startsWith("цель:") -> {

                val value = text
                    .substringAfter(":")
                    .trim()

                if (value.isNotBlank()) {
                    memory.goal = value
                }
            }

            lower.startsWith("term ") ||
                    lower.startsWith("термин ") -> {

                val raw = text
                    .substringAfter(" ")
                    .trim()

                val parts = raw.split(
                    "=",
                    limit = 2
                )

                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()

                    if (key.isNotBlank() && value.isNotBlank()) {
                        memory.fixedTerms[key] = value
                    }
                }
            }

            lower.startsWith("constraint:") ||
                    lower.startsWith("ограничение:") -> {

                val value = text
                    .substringAfter(":")
                    .trim()

                if (
                    value.isNotBlank() &&
                    value !in memory.constraints
                ) {
                    memory.constraints.add(value)
                }
            }

            lower.contains("запомни") ||
                    lower.contains("уточнение") -> {

                if (text !in memory.userClarifications) {
                    memory.userClarifications.add(text)
                }
            }
        }
    }
}
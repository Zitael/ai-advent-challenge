package ru.maleks.ai_advent_challenge_app.rag.chat

class RagTaskMemoryUpdater {

    fun update(userInput: String, memory: RagTaskMemory) {
        val text = userInput.trim()
        val lower = text.lowercase()

        when {
            lower.startsWith("goal:") || lower.startsWith("цель:") -> {
                val value = text.substringAfter(":").trim()
                if (value.isNotBlank()) {
                    memory.userClarifications.add("Goal clarified: $value")
                }
            }

            lower.startsWith("term ") || lower.startsWith("термин ") -> {
                val raw = text.substringAfter(" ").trim()
                val parts = raw.split("=", limit = 2)
                if (parts.size == 2) {
                    memory.fixedTerms[parts[0].trim()] = parts[1].trim()
                }
            }

            lower.startsWith("constraint:") || lower.startsWith("ограничение:") -> {
                val value = text.substringAfter(":").trim()
                if (value.isNotBlank()) {
                    memory.constraints.add(value)
                }
            }

            lower.contains("запомни") || lower.contains("уточнение") -> {
                memory.userClarifications.add(text)
            }
        }
    }
}
package ru.maleks.ai_advent_challenge_app.dataset

class DatasetCleaner(
    private val minUserLength: Int = 20,
    private val maxUserLength: Int = 500
) {

    fun clean(examples: List<FineTuningExample>): List<FineTuningExample> {
        return examples
            .asSequence()
            .filter { example -> isValidStructure(example) }
            .filter { example -> isValidUserLength(example) }
            .filter { example -> isValidAssistantLabel(example) }
            .distinctBy { example -> deduplicationKey(example) }
            .toList()
    }

    private fun isValidStructure(example: FineTuningExample): Boolean {
        if (example.messages.size != 3) {
            return false
        }

        val roles = example.messages.map { it.role.lowercase() }
        return roles == listOf("system", "user", "assistant")
    }

    private fun isValidUserLength(example: FineTuningExample): Boolean {
        val userText = example.messages[1].content.trim()
        return userText.length in minUserLength..maxUserLength
    }

    private fun isValidAssistantLabel(example: FineTuningExample): Boolean {
        val label = example.messages[2].content.trim().lowercase()
        return TicketCategory.fromLabel(label) != null
    }

    private fun deduplicationKey(example: FineTuningExample): String {
        val user = example.messages[1].content.trim().lowercase()
        val assistant = example.messages[2].content.trim().lowercase()
        return "$user|$assistant"
    }
}

package ru.maleks.ai_advent_challenge_app.decomposition

class CompactToonParser {

    fun parse(raw: String): Map<String, String> {
        val payload = extractPayload(raw)
        if (payload.isBlank()) {
            return emptyMap()
        }

        return payload.split('|')
            .mapNotNull { chunk ->
                val separatorIndex = chunk.indexOf('=')
                if (separatorIndex <= 0) {
                    return@mapNotNull null
                }

                val key = chunk.substring(0, separatorIndex).trim().lowercase()
                val value = chunk.substring(separatorIndex + 1).trim()
                key to value
            }
            .toMap()
    }

    fun parseNormalizedTicket(raw: String): NormalizedTicket? {
        val fields = parse(raw)
        val intent = TicketIntent.fromRaw(fields["intent"]) ?: return null
        val cleanText = fields["clean"].orEmpty().ifBlank { fields["clean_text"].orEmpty() }
        if (cleanText.isBlank()) {
            return null
        }

        val signals = fields["signals"]
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return NormalizedTicket(
            intent = intent,
            signals = signals,
            cleanText = cleanText
        )
    }

    fun parseDecision(raw: String): TriageDecision? {
        val fields = parse(raw)
        val category = fields["category"]?.lowercase()?.trim().orEmpty()
        val priority = TicketPriority.fromLabel(fields["priority"]) ?: return null
        val action = TicketAction.fromLabel(fields["action"]) ?: return null

        if (category.isBlank()) {
            return null
        }

        return TriageDecision(
            category = category,
            priority = priority,
            action = action
        )
    }

    fun parseFinalTriage(raw: String): TriageResult? {
        val fields = parse(raw)
        val category = fields["category"]?.lowercase()?.trim().orEmpty()
        val priority = TicketPriority.fromLabel(fields["priority"]) ?: return null
        val action = TicketAction.fromLabel(fields["action"]) ?: return null
        val summary = fields["summary"].orEmpty().trim()

        if (category.isBlank() || summary.isBlank()) {
            return null
        }

        return TriageResult(
            category = category,
            priority = priority,
            action = action,
            summary = summary
        )
    }

    private fun extractPayload(raw: String): String {
        val trimmed = raw.trim()
        val withoutFence = if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```toon")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        } else {
            trimmed
        }

        return withoutFence.lineSequence()
            .firstOrNull { line -> line.contains('=') && line.contains('|') }
            ?: withoutFence.lines().firstOrNull { it.contains('=') }
            ?: withoutFence
    }
}

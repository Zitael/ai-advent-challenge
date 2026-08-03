package ru.maleks.ai_advent_challenge_app.micromodel

import ru.maleks.ai_advent_challenge_app.dataset.TicketCategory

class TicketKeywordMicroClassifier {

    fun classify(ticketText: String): MicroClassificationResult {
        val startedAt = System.nanoTime()
        val normalized = ticketText.lowercase()

        val scored = TicketCategory.entries.associateWith { category ->
            val matched = KEYWORDS[category].orEmpty().filter { keyword ->
                normalized.contains(keyword)
            }
            category to matched
        }

        val ranked = scored.values
            .map { (category, matches) -> Triple(category, matches.size.toDouble(), matches) }
            .sortedByDescending { it.second }

        val top = ranked.first()
        val secondScore = ranked.getOrNull(1)?.second ?: 0.0
        val totalScore = ranked.sumOf { it.second }

        val latencyMs = (System.nanoTime() - startedAt) / 1_000_000

        if (totalScore <= 0.0) {
            return MicroClassificationResult(
                category = null,
                confidence = 0.0,
                status = MicroModelStatus.UNSURE,
                matchedSignals = emptyList(),
                latencyMs = latencyMs
            )
        }

        val confidence = top.second / totalScore
        val margin = (top.second - secondScore) / totalScore
        val activeDomains = ranked.count { it.second > 0.0 }

        val status = when {
            top.second < MIN_TOP_SIGNALS -> MicroModelStatus.UNSURE
            activeDomains > 1 -> MicroModelStatus.UNSURE
            confidence < MIN_CONFIDENCE -> MicroModelStatus.UNSURE
            margin < MIN_MARGIN -> MicroModelStatus.UNSURE
            else -> MicroModelStatus.OK
        }

        val category = if (status == MicroModelStatus.OK) top.first.label else null

        return MicroClassificationResult(
            category = category,
            confidence = confidence,
            status = status,
            matchedSignals = top.third,
            latencyMs = latencyMs
        )
    }

    private companion object {
        const val MIN_TOP_SIGNALS = 1.0
        const val MIN_MARGIN = 0.25
        const val MIN_CONFIDENCE = 0.55

        val KEYWORDS = mapOf(
            TicketCategory.BILLING to listOf(
                "оплат", "плат", "billing", "invoice", "refund", "сч", "тариф",
                "payment", "charge", "promo", "промокод", "денег", "спис"
            ),
            TicketCategory.ACCOUNT to listOf(
                "аккаунт", "account", "login", "log in", "вход", "парол", "password",
                "sso", "профил", "workspace", "email", "2fa", "phone", "логин"
            ),
            TicketCategory.TECHNICAL to listOf(
                "api", "500", "502", "timeout", "error", "deploy", "mcp", "rag",
                "gradle", "ollama", "crash", "health", "export", "pdf", "bug"
            ),
            TicketCategory.FEATURE_REQUEST to listOf(
                "webhook", "feature", "dark mode", "import", "csv", "sla",
                "dashboard", "telegram", "auditor", "endpoint", "bulk", "template"
            )
        )
    }
}

package ru.maleks.ai_advent_challenge_app.decomposition

object DecompositionPrompts {
    val ANALYZE_SYSTEM = """
        You normalize support tickets.
        Reply with compact TOON on one line only:
        intent=<billing|account|technical|feature|mixed>|signals=<comma-separated-keywords>|clean=<normalized ticket text>
        No markdown, no JSON, no explanations.
    """.trimIndent()

    val DECIDE_SYSTEM = """
        You triage normalized support tickets.
        Allowed categories: billing, account, technical, feature_request.
        Allowed priority: low, medium, high, urgent.
        Allowed action: auto_reply, escalate, human_review.
        Reply with compact TOON on one line only:
        category=<...>|priority=<...>|action=<...>
        Rules:
        - billing/account access conflicts -> human_review
        - production outage keywords -> urgent + escalate
        - clear FAQ-level requests -> auto_reply
    """.trimIndent()

    val FORMAT_SYSTEM = """
        You format triage decisions for CRM.
        Reply with compact TOON on one line only:
        category=<...>|priority=<...>|action=<...>|summary=<max 80 chars>
        Keep category/priority/action unchanged.
    """.trimIndent()

    val MONOLITHIC_SYSTEM = """
        You triage support tickets in one step.
        Allowed categories: billing, account, technical, feature_request.
        Allowed priority: low, medium, high, urgent.
        Allowed action: auto_reply, escalate, human_review.
        Reply with compact TOON on one line only:
        category=<...>|priority=<...>|action=<...>|summary=<max 80 chars>
        No markdown, no JSON, no explanations.
    """.trimIndent()

    fun analyzeUser(ticketText: String): String =
        "Ticket: ${ticketText.trim()}"

    fun decideUser(normalized: NormalizedTicket): String =
        """
        intent=${normalized.intent.name.lowercase()}
        signals=${normalized.signals.joinToString(",")}
        clean=${normalized.cleanText}
        """.trimIndent()

    fun formatUser(normalized: NormalizedTicket, decision: TriageDecision): String =
        """
        clean=${normalized.cleanText}
        category=${decision.category}
        priority=${decision.priority.label}
        action=${decision.action.label}
        """.trimIndent()
}

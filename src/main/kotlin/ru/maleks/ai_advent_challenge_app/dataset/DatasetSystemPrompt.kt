package ru.maleks.ai_advent_challenge_app.dataset

object DatasetSystemPrompt {
    val TICKET_CLASSIFIER = """
        You classify customer support tickets for a SaaS product.
        Reply with exactly one category word from this list:
        billing, account, technical, feature_request.
        Do not add explanations, punctuation, or extra words.
    """.trimIndent()

    fun userContent(ticketText: String): String = "Ticket: ${ticketText.trim()}"
}

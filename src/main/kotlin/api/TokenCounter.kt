package api

import kotlin.math.ceil

class TokenCounter {

    fun estimateTextTokens(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        return ceil(text.length / 4.0).toInt()
    }

    fun estimateMessagesTokens(messages: List<OpenRouterMessage>): Int {
        return messages.sumOf { message ->
            estimateTextTokens(message.role) + estimateTextTokens(message.content) + 4
        }
    }
}
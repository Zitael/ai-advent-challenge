package ru.maleks.ai_advent_challenge_app.privateai.session

import java.util.concurrent.ConcurrentHashMap

class ChatSessionStore(
    private val maxHistoryMessages: Int
) {
    private val sessions =
        ConcurrentHashMap<String, MutableList<PrivateChatMessage>>()

    fun getHistory(sessionId: String): List<PrivateChatMessage> {
        val messages = sessions[sessionId]
            ?: return emptyList()

        return synchronized(messages) {
            messages
                .takeLast(maxHistoryMessages)
                .toList()
        }
    }

    fun append(
        sessionId: String,
        userMessage: String,
        assistantMessage: String
    ) {
        val messages = sessions.computeIfAbsent(sessionId) {
            mutableListOf()
        }

        synchronized(messages) {
            messages += PrivateChatMessage(
                role = "user",
                content = userMessage
            )

            messages += PrivateChatMessage(
                role = "assistant",
                content = assistantMessage
            )

            trim(messages)
        }
    }

    fun clear(sessionId: String): Int {
        return sessions.remove(sessionId)?.size ?: 0
    }

    fun size(sessionId: String): Int {
        return sessions[sessionId]?.let { messages ->
            synchronized(messages) {
                messages.size
            }
        } ?: 0
    }

    private fun trim(messages: MutableList<PrivateChatMessage>) {
        val excess = messages.size - maxHistoryMessages

        if (excess > 0) {
            repeat(excess) {
                messages.removeAt(0)
            }
        }
    }
}
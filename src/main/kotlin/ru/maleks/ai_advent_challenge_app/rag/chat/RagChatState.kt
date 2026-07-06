package ru.maleks.ai_advent_challenge_app.rag.chat

data class RagChatState(
    val messages: MutableList<RagChatMessage> = mutableListOf(),
    val taskMemory: RagTaskMemory = RagTaskMemory()
)
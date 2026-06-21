package ru.maleks.ai_advent_challenge_app.archive.api

data class ConversationState(
    var summary: String = "",
    var messages: MutableList<OpenRouterMessage> = mutableListOf()
)
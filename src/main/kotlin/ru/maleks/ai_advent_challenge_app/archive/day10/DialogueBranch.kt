package ru.maleks.ai_advent_challenge_app.archive.day10

import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage

data class DialogueBranch(
    val name: String,
    val messages: MutableList<OpenRouterMessage> = mutableListOf()
)
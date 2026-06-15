package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage

data class DialogueBranch(
    val name: String,
    val messages: MutableList<OpenRouterMessage> = mutableListOf()
)
package ru.maleks.ai_advent_challenge_app.archive.day10

import ru.maleks.ai_advent_challenge_app.archive.api.Usage

data class LlmResult(
    val answer: String,
    val usage: Usage?
)
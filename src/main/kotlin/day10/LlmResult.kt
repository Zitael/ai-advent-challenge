package ru.ai_advent_app.day1.day10

import api.Usage

data class LlmResult(
    val answer: String,
    val usage: Usage?
)
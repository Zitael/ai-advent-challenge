package ru.maleks.ai_advent_challenge_app.invariant

data class Invariant(
    val id: String,
    val description: String,
    val forbiddenKeywords: List<String> = emptyList()
)
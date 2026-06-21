package ru.maleks.ai_advent_challenge_app.invariant

data class InvariantViolation(
    val invariantId: String,
    val invariantDescription: String,
    val matchedKeywords: List<String>
)
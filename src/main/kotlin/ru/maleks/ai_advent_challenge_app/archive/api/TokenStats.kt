package ru.maleks.ai_advent_challenge_app.archive.api

data class TokenStats(
    val currentRequestEstimatedTokens: Int,
    val historyEstimatedTokens: Int,
    val responseEstimatedTokens: Int,
    val apiPromptTokens: Int?,
    val apiCompletionTokens: Int?,
    val apiTotalTokens: Int?,
    val apiCost: Double?
)
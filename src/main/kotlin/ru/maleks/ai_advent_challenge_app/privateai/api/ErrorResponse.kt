package ru.maleks.ai_advent_challenge_app.privateai.api

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String
)
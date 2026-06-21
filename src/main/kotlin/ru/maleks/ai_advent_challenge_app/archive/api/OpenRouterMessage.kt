package ru.maleks.ai_advent_challenge_app.archive.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterMessage(
    val role: String,
    val content: String
)
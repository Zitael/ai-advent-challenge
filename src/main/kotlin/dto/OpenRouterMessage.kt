package ru.ai_advent_app.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterMessage(
    val role: String,
    val content: String
)
package ru.ai_advent_app.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)
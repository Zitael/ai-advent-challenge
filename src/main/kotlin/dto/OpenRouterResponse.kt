package ru.ai_advent_app.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterResponse(
    val choices: List<Choice>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Choice(
    val message: OpenRouterMessage
)
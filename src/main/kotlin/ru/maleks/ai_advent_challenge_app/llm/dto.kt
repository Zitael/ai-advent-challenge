package ru.maleks.ai_advent_challenge_app.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,

    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,

    val temperature: Double? = null,
    val stop: List<String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterMessage(
    val role: String,
    val content: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Choice(
    val message: OpenRouterMessage
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Usage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int? = null,

    @JsonProperty("completion_tokens")
    val completionTokens: Int? = null,

    @JsonProperty("total_tokens")
    val totalTokens: Int? = null,

    val cost: Double? = null
)
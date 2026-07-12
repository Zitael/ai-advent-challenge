package ru.maleks.ai_advent_challenge_app.llm.ollama

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaChatResponse(
    val model: String = "",

    @JsonProperty("created_at")
    val createdAt: String = "",

    val message: OllamaMessage = OllamaMessage(
        role = "assistant",
        content = ""
    ),

    val done: Boolean = false,

    @JsonProperty("done_reason")
    val doneReason: String? = null,

    @JsonProperty("total_duration")
    val totalDuration: Long? = null,

    @JsonProperty("load_duration")
    val loadDuration: Long? = null,

    @JsonProperty("prompt_eval_count")
    val promptEvalCount: Int? = null,

    @JsonProperty("prompt_eval_duration")
    val promptEvalDuration: Long? = null,

    @JsonProperty("eval_count")
    val evalCount: Int? = null,

    @JsonProperty("eval_duration")
    val evalDuration: Long? = null
)
package api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterResponse(
    val id: String? = null,
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
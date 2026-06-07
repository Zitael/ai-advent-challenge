package api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val temperature: Double? = null
)
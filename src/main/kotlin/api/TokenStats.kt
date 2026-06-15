package api

data class TokenStats(
    val currentRequestEstimatedTokens: Int,
    val historyEstimatedTokens: Int,
    val responseEstimatedTokens: Int,
    val apiPromptTokens: Int?,
    val apiCompletionTokens: Int?,
    val apiTotalTokens: Int?,
    val apiCost: Double?
)
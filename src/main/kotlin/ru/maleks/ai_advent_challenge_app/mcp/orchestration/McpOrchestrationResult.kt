package ru.maleks.ai_advent_challenge_app.mcp.orchestration

data class McpOrchestrationResult(
    val query: String,
    val searchResult: String,
    val summaryResult: String,
    val saveResult: String
)
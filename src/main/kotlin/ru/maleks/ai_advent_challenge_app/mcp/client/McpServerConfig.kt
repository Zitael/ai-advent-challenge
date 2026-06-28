package ru.maleks.ai_advent_challenge_app.mcp.client

data class McpServerConfig(
    val url: String,
    val clientName: String = "ai-advent-challenge-client",
    val clientVersion: String = "1.0.0"
)
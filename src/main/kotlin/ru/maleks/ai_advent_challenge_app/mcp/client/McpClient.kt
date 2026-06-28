package ru.maleks.ai_advent_challenge_app.mcp.client

interface McpClient {
    suspend fun listTools(): List<McpToolInfo>
}
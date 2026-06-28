package ru.maleks.ai_advent_challenge_app.mcp.client

interface McpClient {
    suspend fun listTools(): List<McpToolInfo>

    suspend fun callTool(
        name: String,
        arguments: Map<String, Any?>
    ): McpToolCallResult
}
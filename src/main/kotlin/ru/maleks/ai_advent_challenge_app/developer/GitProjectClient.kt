package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.mcp.client.McpClient

class GitProjectClient(
    private val mcpClient: McpClient
) {
    suspend fun currentBranch(): String = call("git_current_branch")

    suspend fun status(): String = call("git_status")

    suspend fun diff(): String = call("git_diff")

    suspend fun files(): String = call("project_files")

    private suspend fun call(toolName: String): String {
        return mcpClient.callTool(
            name = toolName,
            arguments = emptyMap()
        ).text.trim()
    }
}

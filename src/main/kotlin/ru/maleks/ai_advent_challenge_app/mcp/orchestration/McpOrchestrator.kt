package ru.maleks.ai_advent_challenge_app.mcp.orchestration

import ru.maleks.ai_advent_challenge_app.mcp.client.McpServerConfig
import ru.maleks.ai_advent_challenge_app.mcp.client.RemoteMcpClient
import java.time.Instant

class McpOrchestrator(
    private val router: McpToolRouter
) {

    suspend fun runLongFlow(query: String): McpOrchestrationResult {
        println()
        println("========== MCP ORCHESTRATION START ==========")

        val searchResult = call(
            stepName = "Step 1",
            toolName = "search_mock_tasks",
            arguments = mapOf("query" to query)
        )

        val summaryResult = call(
            stepName = "Step 2",
            toolName = "summarize_tasks",
            arguments = mapOf("tasksText" to searchResult)
        )

        val fileName = "orchestrated-report-${Instant.now().toEpochMilli()}.txt"

        val saveResult = call(
            stepName = "Step 3",
            toolName = "save_text_to_file",
            arguments = mapOf(
                "fileName" to fileName,
                "content" to summaryResult
            )
        )

        println("=========== MCP ORCHESTRATION END ===========")
        println()

        return McpOrchestrationResult(
            query = query,
            searchResult = searchResult,
            summaryResult = summaryResult,
            saveResult = saveResult
        )
    }

    private suspend fun call(
        stepName: String,
        toolName: String,
        arguments: Map<String, Any?>
    ): String {
        val route = router.route(toolName)

        println("$stepName: $toolName")
        println("Server: ${route.serverName} (${route.serverUrl})")

        val client = RemoteMcpClient(
            config = McpServerConfig(
                url = route.serverUrl,
                clientName = "ai-advent-orchestrator",
                clientVersion = "1.0.0"
            )
        )

        val result = client.callTool(
            name = toolName,
            arguments = arguments
        )

        println(result.text)
        println()

        return result.text
    }
}
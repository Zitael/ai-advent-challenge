package ru.maleks.ai_advent_challenge_app.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.maleks.ai_advent_challenge_app.mcp.server.scheduler.TaskSummaryScheduler

class LocalMcpServerFactory(
    private val mockTaskApi: MockTaskApi,
    private val taskSummaryScheduler: TaskSummaryScheduler
) {

    fun create(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "ai-advent-local-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        server.addTool(
            name = "get_mock_task",
            description = "Get mock tracker task by task id. Example ids: AIA-1, AIA-2, AIA-3.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put(
                        "taskId",
                        buildJsonObject {
                            put("type", "string")
                            put("description", "Task id, for example AIA-2")
                        }
                    )
                },
                required = listOf("taskId")
            )
        ) { request ->
            val taskId = request.params.arguments?.get("taskId")
                ?.jsonPrimitive
                ?.content
                ?.trim()
                .orEmpty()

            if (taskId.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Error: taskId is required. Example: AIA-2"
                        )
                    )
                )
            }

            val task = mockTaskApi.getTask(taskId)

            if (task == null) {
                return@addTool CallToolResult(
                    content = listOf(
                        TextContent(
                            text = "Task not found: $taskId. Available examples: AIA-1, AIA-2, AIA-3."
                        )
                    )
                )
            }

            val result = """
                Task:
                - id: ${task.id}
                - title: ${task.title}
                - status: ${task.status}
                - priority: ${task.priority}
                - assignee: ${task.assignee}
                - description: ${task.description}
            """.trimIndent()

            CallToolResult(
                content = listOf(
                    TextContent(text = result)
                )
            )
        }

        server.addTool(
            name = "get_periodic_task_summary",
            description = "Return aggregated result collected periodically by background scheduler.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put(
                        "limit",
                        buildJsonObject {
                            put("type", "number")
                            put("description", "Number of recent scheduler snapshots to return. Default is 5.")
                        }
                    )
                },
                required = emptyList()
            )
        ) { request ->
            val limit = request.params.arguments?.get("limit")
                ?.jsonPrimitive
                ?.intOrNull
                ?: 5

            val safeLimit = limit.coerceIn(1, 20)

            CallToolResult(
                content = listOf(
                    TextContent(
                        text = taskSummaryScheduler.getAggregatedSummary(safeLimit)
                    )
                )
            )
        }

        return server
    }
}
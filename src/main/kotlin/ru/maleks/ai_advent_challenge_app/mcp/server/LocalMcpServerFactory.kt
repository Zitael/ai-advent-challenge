package ru.maleks.ai_advent_challenge_app.mcp.server

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.maleks.ai_advent_challenge_app.mcp.server.scheduler.TaskSummaryScheduler
import java.io.File
import java.time.Instant

class LocalMcpServerFactory(
    private val config: LocalMcpServerConfig,
    private val mockTaskApi: MockTaskApi,
    private val taskSummaryScheduler: TaskSummaryScheduler
) {

    fun create(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "ai-advent-${config.kind.name.lowercase()}-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        when (config.kind) {
            LocalMcpServerKind.TRACKER -> registerTrackerTools(server)
            LocalMcpServerKind.REPORT -> registerReportTools(server)
        }

        return server
    }

    private fun registerTrackerTools(server: Server) {
        server.addTool(
            name = "get_mock_task",
            description = "Get mock tracker task by task id. Example ids: AIA-1, AIA-2, AIA-3.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("taskId", stringProperty("Task id, for example AIA-2"))
                },
                required = listOf("taskId")
            )
        ) { request ->
            val taskId = request.params.arguments?.readString("taskId") ?: ""

            if (taskId.isBlank()) {
                return@addTool textResult("Error: taskId is required. Example: AIA-2")
            }

            val task = mockTaskApi.getTask(taskId)

            if (task == null) {
                return@addTool textResult(
                    "Task not found: $taskId. Available examples: AIA-1, AIA-2, AIA-3."
                )
            }

            textResult(formatTask(task))
        }

        server.addTool(
            name = "search_mock_tasks",
            description = "Search mock tracker tasks by text query. Searches in id, title, status, priority, assignee and description.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("query", stringProperty("Search query, for example: HIGH, MCP, TODO, Maxim"))
                },
                required = listOf("query")
            )
        ) { request ->
            val query = request.params.arguments?.readString("query") ?: ""

            if (query.isBlank()) {
                return@addTool textResult("Error: query is required.")
            }

            val normalizedQuery = query.lowercase()

            val foundTasks = mockTaskApi.getAllTasks()
                .filter { task ->
                    listOf(
                        task.id,
                        task.title,
                        task.status,
                        task.priority,
                        task.assignee,
                        task.description
                    ).any { value -> value.lowercase().contains(normalizedQuery) }
                }

            if (foundTasks.isEmpty()) {
                textResult("No tasks found for query: $query")
            } else {
                textResult(
                    buildString {
                        appendLine("Search query: $query")
                        appendLine("Found tasks: ${foundTasks.size}")
                        appendLine()

                        foundTasks.forEach { task ->
                            appendLine(formatTask(task))
                            appendLine()
                        }
                    }.trim()
                )
            }
        }

        server.addTool(
            name = "get_periodic_task_summary",
            description = "Return aggregated result collected periodically by background scheduler.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("limit", numberProperty("Number of recent scheduler snapshots to return. Default is 5."))
                },
                required = emptyList()
            )
        ) { request ->
            val limit = request.params.arguments?.readInt("limit") ?: 5
            val safeLimit = limit.coerceIn(1, 20)

            textResult(taskSummaryScheduler.getAggregatedSummary(safeLimit))
        }
    }

    private fun registerReportTools(server: Server) {
        server.addTool(
            name = "summarize_tasks",
            description = "Summarize task search result text into a concise status report.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("tasksText", stringProperty("Raw task text returned by search_mock_tasks"))
                },
                required = listOf("tasksText")
            )
        ) { request ->
            val tasksText = request.params.arguments?.readString("tasksText") ?: ""

            if (tasksText.isBlank()) {
                return@addTool textResult("Error: tasksText is required.")
            }

            val highPriorityCount = Regex("priority: HIGH", RegexOption.IGNORE_CASE)
                .findAll(tasksText)
                .count()

            val todoCount = Regex("status: TODO", RegexOption.IGNORE_CASE)
                .findAll(tasksText)
                .count()

            val inProgressCount = Regex("status: IN_PROGRESS", RegexOption.IGNORE_CASE)
                .findAll(tasksText)
                .count()

            val doneCount = Regex("status: DONE", RegexOption.IGNORE_CASE)
                .findAll(tasksText)
                .count()

            val taskIds = Regex("id: ([A-Z]+-\\d+)")
                .findAll(tasksText)
                .map { it.groupValues[1] }
                .toList()

            val summary = """
                Task pipeline summary:
                - tasks found: ${taskIds.size}
                - task ids: ${taskIds.joinToString(", ").ifBlank { "none" }}
                - high priority: $highPriorityCount
                - todo: $todoCount
                - in progress: $inProgressCount
                - done: $doneCount

                Recommendation:
                ${buildRecommendation(highPriorityCount, inProgressCount, todoCount)}
            """.trimIndent()

            textResult(summary)
        }

        server.addTool(
            name = "save_text_to_file",
            description = "Save text content to local file and return saved file path.",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    put("fileName", stringProperty("File name, for example task-summary.txt"))
                    put("content", stringProperty("Text content to save"))
                },
                required = listOf("fileName", "content")
            )
        ) { request ->
            val fileName = request.params.arguments?.readString("fileName") ?: ""
            val content = request.params.arguments?.readString("content") ?: ""

            if (fileName.isBlank()) {
                return@addTool textResult("Error: fileName is required.")
            }

            if (content.isBlank()) {
                return@addTool textResult("Error: content is required.")
            }

            val safeFileName = fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_")

            val directory = File("mcp-pipeline-output")
            directory.mkdirs()

            val file = File(directory, safeFileName)
            file.writeText(
                """
                    Saved at: ${Instant.now()}
                    
                    $content
                """.trimIndent()
            )

            textResult(
                """
                    File saved successfully.
                    Path: ${file.absolutePath}
                    Size: ${file.length()} bytes
                """.trimIndent()
            )
        }
    }

    private fun Map<String, JsonElement>.readString(key: String): String {
        return this[key]
            ?.jsonPrimitive
            ?.content
            ?.trim()
            .orEmpty()
    }

    private fun Map<String, JsonElement>.readInt(key: String): Int? {
        return this[key]
            ?.jsonPrimitive
            ?.intOrNull
    }

    private fun stringProperty(description: String) = buildJsonObject {
        put("type", "string")
        put("description", description)
    }

    private fun numberProperty(description: String) = buildJsonObject {
        put("type", "number")
        put("description", description)
    }

    private fun textResult(text: String): CallToolResult {
        return CallToolResult(
            content = listOf(
                TextContent(text = text)
            )
        )
    }

    private fun formatTask(task: MockTask): String {
        return """
            Task:
            - id: ${task.id}
            - title: ${task.title}
            - status: ${task.status}
            - priority: ${task.priority}
            - assignee: ${task.assignee}
            - description: ${task.description}
        """.trimIndent()
    }

    private fun buildRecommendation(
        highPriorityCount: Int,
        inProgressCount: Int,
        todoCount: Int
    ): String {
        return when {
            highPriorityCount > 0 && inProgressCount > 0 ->
                "Focus on active high-priority work first and avoid starting new tasks."

            highPriorityCount > 0 && todoCount > 0 ->
                "Start with high-priority TODO tasks and move them into execution."

            inProgressCount > 0 ->
                "Finish current in-progress tasks before expanding scope."

            else ->
                "No urgent action detected. Continue monitoring the task list."
        }
    }
}
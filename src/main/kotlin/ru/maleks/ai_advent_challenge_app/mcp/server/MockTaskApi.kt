package ru.maleks.ai_advent_challenge_app.mcp.server

class MockTaskApi {

    private val tasks = mapOf(
        "AIA-1" to MockTask(
            id = "AIA-1",
            title = "Add MCP client",
            status = "DONE",
            priority = "MEDIUM",
            assignee = "Maxim",
            description = "Connect to remote MCP server and list available tools."
        ),
        "AIA-2" to MockTask(
            id = "AIA-2",
            title = "Create first MCP tool",
            status = "IN_PROGRESS",
            priority = "HIGH",
            assignee = "Maxim",
            description = "Implement local MCP server with a mock task API tool."
        ),
        "AIA-3" to MockTask(
            id = "AIA-3",
            title = "Integrate MCP tool with agent",
            status = "TODO",
            priority = "HIGH",
            assignee = "Maxim",
            description = "Call MCP tool from the application and pass result to the AI agent."
        )
    )

    fun getTask(taskId: String): MockTask? {
        return tasks[taskId.uppercase()]
    }
}
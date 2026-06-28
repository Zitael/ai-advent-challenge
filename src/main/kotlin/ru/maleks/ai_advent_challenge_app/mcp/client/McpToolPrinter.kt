package ru.maleks.ai_advent_challenge_app.mcp.client

object McpToolPrinter {

    fun print(serverUrl: String, tools: List<McpToolInfo>) {
        println()
        println("========== MCP TOOLS ==========")
        println("Server: $serverUrl")
        println("Tools count: ${tools.size}")
        println()

        if (tools.isEmpty()) {
            println("No tools returned.")
        } else {
            tools.forEachIndexed { index, tool ->
                println("${index + 1}. ${tool.name}")
                println("   description: ${tool.description.ifBlank { "empty" }}")
                println("   input schema: ${tool.inputSchema}")
                println()
            }
        }

        println("===============================")
        println()
    }
}
package ru.maleks.ai_advent_challenge_app.mcp.orchestration

class McpToolRouter(
    servers: List<McpServerRef>,
    routes: List<McpToolRoute>
) {
    private val serverByName = servers.associateBy { it.name }
    private val routeByTool = routes.associateBy { it.toolName }

    fun route(toolName: String): McpToolRoute {
        return routeByTool[toolName]
            ?: error("No MCP route configured for tool: $toolName")
    }

    fun printRoutes() {
        println()
        println("========== MCP ROUTES ==========")

        if (routeByTool.isEmpty()) {
            println("empty")
        } else {
            routeByTool.values.forEach { route ->
                println("- ${route.toolName} -> ${route.serverName} (${route.serverUrl})")
            }
        }

        println("================================")
        println()
    }

    fun printServers() {
        println()
        println("========== MCP SERVERS ==========")

        if (serverByName.isEmpty()) {
            println("empty")
        } else {
            serverByName.values.forEach { server ->
                println("- ${server.name}: ${server.url}")
            }
        }

        println("=================================")
        println()
    }
}
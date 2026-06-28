package ru.maleks.ai_advent_challenge_app

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.agent.LayeredMemoryAgent
import ru.maleks.ai_advent_challenge_app.invariant.Invariant
import ru.maleks.ai_advent_challenge_app.invariant.InvariantChecker
import ru.maleks.ai_advent_challenge_app.invariant.InvariantStorage
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.mcp.client.McpServerConfig
import ru.maleks.ai_advent_challenge_app.mcp.client.McpToolPrinter
import ru.maleks.ai_advent_challenge_app.mcp.client.RemoteMcpClient
import ru.maleks.ai_advent_challenge_app.mcp.orchestration.McpOrchestrator
import ru.maleks.ai_advent_challenge_app.mcp.orchestration.McpServerRef
import ru.maleks.ai_advent_challenge_app.mcp.orchestration.McpToolRoute
import ru.maleks.ai_advent_challenge_app.mcp.orchestration.McpToolRouter
import ru.maleks.ai_advent_challenge_app.mcp.server.LocalMcpServerConfig
import ru.maleks.ai_advent_challenge_app.mcp.server.LocalMcpServerFactory
import ru.maleks.ai_advent_challenge_app.mcp.server.LocalMcpServerKind
import ru.maleks.ai_advent_challenge_app.mcp.server.LocalMcpServerRunner
import ru.maleks.ai_advent_challenge_app.mcp.server.MockTaskApi
import ru.maleks.ai_advent_challenge_app.mcp.server.scheduler.TaskSummaryScheduler
import ru.maleks.ai_advent_challenge_app.memory.AssistantMemoryStorage
import ru.maleks.ai_advent_challenge_app.profile.UserProfileStorage
import ru.maleks.ai_advent_challenge_app.state.TaskStage
import ru.maleks.ai_advent_challenge_app.state.TaskStateMachine
import ru.maleks.ai_advent_challenge_app.state.TaskStateStorage

suspend fun main() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val model = dotenv["OPENROUTER_MODEL"]
        ?: System.getenv("OPENROUTER_MODEL")
        ?: "openai/gpt-4o-mini"

    val publicMcpServerUrl = dotenv["MCP_SERVER_URL"]
        ?: System.getenv("MCP_SERVER_URL")

    val mockTaskApi = MockTaskApi()

    val taskSummaryScheduler = TaskSummaryScheduler(
        mockTaskApi = mockTaskApi,
        intervalSeconds = 10
    )

    taskSummaryScheduler.start()

    val trackerConfig = LocalMcpServerConfig(
        kind = LocalMcpServerKind.TRACKER,
        host = "127.0.0.1",
        port = 3000,
        path = "/mcp"
    )

    val reportConfig = LocalMcpServerConfig(
        kind = LocalMcpServerKind.REPORT,
        host = "127.0.0.1",
        port = 3001,
        path = "/mcp"
    )

    val trackerServerRunner = LocalMcpServerRunner(
        config = trackerConfig,
        factory = LocalMcpServerFactory(
            config = trackerConfig,
            mockTaskApi = mockTaskApi,
            taskSummaryScheduler = taskSummaryScheduler
        )
    )

    val reportServerRunner = LocalMcpServerRunner(
        config = reportConfig,
        factory = LocalMcpServerFactory(
            config = reportConfig,
            mockTaskApi = mockTaskApi,
            taskSummaryScheduler = taskSummaryScheduler
        )
    )

    trackerServerRunner.start()
    reportServerRunner.start()

    val mcpToolRouter = McpToolRouter(
        servers = listOf(
            McpServerRef(
                name = "tracker",
                url = trackerServerRunner.url()
            ),
            McpServerRef(
                name = "report",
                url = reportServerRunner.url()
            )
        ),
        routes = listOf(
            McpToolRoute(
                toolName = "get_mock_task",
                serverName = "tracker",
                serverUrl = trackerServerRunner.url()
            ),
            McpToolRoute(
                toolName = "search_mock_tasks",
                serverName = "tracker",
                serverUrl = trackerServerRunner.url()
            ),
            McpToolRoute(
                toolName = "get_periodic_task_summary",
                serverName = "tracker",
                serverUrl = trackerServerRunner.url()
            ),
            McpToolRoute(
                toolName = "summarize_tasks",
                serverName = "report",
                serverUrl = reportServerRunner.url()
            ),
            McpToolRoute(
                toolName = "save_text_to_file",
                serverName = "report",
                serverUrl = reportServerRunner.url()
            )
        )
    )

    val mcpOrchestrator = McpOrchestrator(
        router = mcpToolRouter
    )

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    val llmClient = OpenRouterClient(
        httpClient = httpClient,
        apiKey = apiKey,
        model = model
    )

    val memoryStorage = AssistantMemoryStorage()
    val profileStorage = UserProfileStorage()
    val taskStateStorage = TaskStateStorage()
    val taskStateMachine = TaskStateMachine(taskStateStorage)
    val invariantStorage = InvariantStorage()
    val invariantChecker = InvariantChecker(invariantStorage)

    val profiles = profileStorage.loadProfiles()
    var activeProfile = profiles["backend"] ?: profiles.values.first()

    val agent = LayeredMemoryAgent(
        name = "LayeredMemoryAgent",
        llmClient = llmClient,
        memoryStorage = memoryStorage,
        userProfile = activeProfile,
        taskStateMachine = taskStateMachine,
        invariantChecker = invariantChecker
    )

    println("AI Advent Challenge — Day 20")
    println("Agent: ${agent.name}")
    println("Model: $model")
    println("Public MCP server: ${publicMcpServerUrl ?: "not configured"}")
    println("Tracker MCP server: ${trackerServerRunner.url()}")
    println("Report MCP server: ${reportServerRunner.url()}")
    println()
    printHelp()

    try {
        while (true) {
            print("You: ")
            val input = readlnOrNull()?.trim()

            if (input.isNullOrBlank()) {
                continue
            }

            when {
                input.equals("exit", ignoreCase = true) -> break

                input.equals("help", ignoreCase = true) -> {
                    printHelp()
                    continue
                }

                input.equals("memory", ignoreCase = true) -> {
                    agent.printMemory()
                    continue
                }

                input.equals("clear", ignoreCase = true) -> {
                    agent.clearMemory()
                    println("Memory cleared.")
                    continue
                }

                input.equals("profiles", ignoreCase = true) -> {
                    println("Available profiles:")
                    profiles.values.forEach { profile ->
                        val marker = if (profile.id == activeProfile.id) "*" else " "
                        println("$marker ${profile.id} — ${profile.name}")
                    }
                    continue
                }

                input.startsWith("profile ", ignoreCase = true) -> {
                    val profileId = input.removePrefixIgnoreCase("profile ").trim()
                    val profile = profiles[profileId]

                    if (profile == null) {
                        println("Profile not found: $profileId")
                    } else {
                        activeProfile = profile
                        agent.setUserProfile(profile)
                        println("Active profile: ${profile.id} — ${profile.name}")
                    }

                    continue
                }

                input.startsWith("remember short ", ignoreCase = true) -> {
                    val text = input.removePrefixIgnoreCase("remember short ").trim()
                    agent.rememberShort(text)
                    println("Saved to short-term memory.")
                    continue
                }

                input.startsWith("remember work ", ignoreCase = true) -> {
                    val pair = input.removePrefixIgnoreCase("remember work ").trim()
                    val parsed = parseKeyValue(pair)

                    if (parsed == null) {
                        println("Invalid format. Use: remember work key=value")
                    } else {
                        agent.rememberWorking(parsed.first, parsed.second)
                        println("Saved to working memory.")
                    }

                    continue
                }

                input.startsWith("remember long ", ignoreCase = true) -> {
                    val pair = input.removePrefixIgnoreCase("remember long ").trim()
                    val parsed = parseKeyValue(pair)

                    if (parsed == null) {
                        println("Invalid format. Use: remember long key=value")
                    } else {
                        agent.rememberLongTerm(parsed.first, parsed.second)
                        println("Saved to long-term memory.")
                    }

                    continue
                }

                input.equals("state", ignoreCase = true) -> {
                    taskStateMachine.printState()
                    continue
                }

                input.startsWith("task ", ignoreCase = true) -> {
                    val description = input.removePrefixIgnoreCase("task ").trim()
                    taskStateMachine.setTask(description)
                    println("Task description saved. Stage: PLANNING")
                    continue
                }

                input.startsWith("step ", ignoreCase = true) -> {
                    val step = input.removePrefixIgnoreCase("step ").trim()
                    taskStateMachine.setCurrentStep(step)
                    println("Current step saved.")
                    continue
                }

                input.startsWith("expect ", ignoreCase = true) -> {
                    val action = input.removePrefixIgnoreCase("expect ").trim()
                    taskStateMachine.setExpectedAction(action)
                    println("Expected action saved.")
                    continue
                }

                input.startsWith("plan ", ignoreCase = true) -> {
                    val plan = input.removePrefixIgnoreCase("plan ").trim()
                    taskStateMachine.setApprovedPlan(plan)
                    println("Approved plan saved.")
                    continue
                }

                input.equals("next", ignoreCase = true) -> {
                    val result = taskStateMachine.next()
                    println(result.message)
                    continue
                }

                input.equals("back", ignoreCase = true) -> {
                    val result = taskStateMachine.back()
                    println(result.message)
                    continue
                }

                input.startsWith("goto ", ignoreCase = true) -> {
                    val rawStage = input.removePrefixIgnoreCase("goto ").trim()
                    val targetStage = parseTaskStage(rawStage)

                    if (targetStage == null) {
                        println("Unknown stage: $rawStage")
                        println("Available stages: planning, execution, validation, done")
                    } else {
                        val result = taskStateMachine.transitionTo(targetStage)
                        println(result.message)
                    }

                    continue
                }

                input.equals("reset-state", ignoreCase = true) -> {
                    taskStateMachine.reset()
                    println("Task state reset.")
                    continue
                }

                input.equals("invariants", ignoreCase = true) -> {
                    invariantChecker.printInvariants()
                    continue
                }

                input.startsWith("invariant ", ignoreCase = true) -> {
                    val raw = input.removePrefixIgnoreCase("invariant ").trim()
                    val parsed = parseInvariant(raw)

                    if (parsed == null) {
                        println("Invalid format. Use: invariant <id>|<description>|<forbidden1,forbidden2>")
                    } else {
                        invariantChecker.add(parsed)
                        println("Invariant saved: ${parsed.id}")
                    }

                    continue
                }

                input.equals("clear-invariants", ignoreCase = true) -> {
                    invariantChecker.clear()
                    println("Invariants cleared.")
                    continue
                }

                input.equals("mcp-routes", ignoreCase = true) -> {
                    mcpToolRouter.printServers()
                    mcpToolRouter.printRoutes()
                    continue
                }

                input.equals("tracker-tools", ignoreCase = true) -> {
                    try {
                        val mcpClient = RemoteMcpClient(
                            config = McpServerConfig(
                                url = trackerServerRunner.url()
                            )
                        )

                        val tools = mcpClient.listTools()
                        McpToolPrinter.print(trackerServerRunner.url(), tools)
                    } catch (e: Exception) {
                        println("Tracker MCP connection failed: ${e.message}")
                    }

                    continue
                }

                input.equals("report-tools", ignoreCase = true) -> {
                    try {
                        val mcpClient = RemoteMcpClient(
                            config = McpServerConfig(
                                url = reportServerRunner.url()
                            )
                        )

                        val tools = mcpClient.listTools()
                        McpToolPrinter.print(reportServerRunner.url(), tools)
                    } catch (e: Exception) {
                        println("Report MCP connection failed: ${e.message}")
                    }

                    continue
                }

                input.startsWith("mcp-orchestrate ", ignoreCase = true) -> {
                    val query = input.removePrefixIgnoreCase("mcp-orchestrate ").trim()

                    if (query.isBlank()) {
                        println("Usage: mcp-orchestrate <query>")
                        continue
                    }

                    try {
                        val orchestrationResult = mcpOrchestrator.runLongFlow(query)

                        val agentPrompt = """
                            A long MCP orchestration flow was executed across multiple MCP servers.

                            Servers:
                            - tracker server: search_mock_tasks
                            - report server: summarize_tasks, save_text_to_file

                            User query:
                            ${orchestrationResult.query}

                            Final summary:
                            ${orchestrationResult.summaryResult}

                            Save result:
                            ${orchestrationResult.saveResult}

                            Explain the routing decisions, tool order and final outcome.
                        """.trimIndent()

                        val response = agent.handle(agentPrompt)

                        println()
                        println("${agent.name}:")
                        println(response)
                        println()
                    } catch (e: Exception) {
                        println("MCP orchestration failed: ${e.message}")
                    }

                    continue
                }
            }

            try {
                val response = agent.handle(input)
                println()
                println("${agent.name}:")
                println(response)
                println()
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    } finally {
        httpClient.close()
        trackerServerRunner.stop()
        reportServerRunner.stop()
        taskSummaryScheduler.stop()
    }
}

private fun printHelp() {
    println("Commands:")
    println("  help")
    println("  memory")
    println("  clear")
    println("  profiles")
    println("  profile <id>")
    println("  remember short <text>")
    println("  remember work <key>=<value>")
    println("  remember long <key>=<value>")
    println("  state")
    println("  task <description>")
    println("  step <text>")
    println("  expect <text>")
    println("  plan <text>")
    println("  next")
    println("  back")
    println("  goto <planning|execution|validation|done>")
    println("  reset-state")
    println("  invariants")
    println("  invariant <id>|<description>|<forbidden1,forbidden2>")
    println("  clear-invariants")
    println("  mcp-routes")
    println("  tracker-tools")
    println("  report-tools")
    println("  mcp-orchestrate <query>")
    println("  exit")
    println()
}

private fun parseKeyValue(input: String): Pair<String, String>? {
    if (!input.contains("=")) {
        return null
    }

    val parts = input.split("=", limit = 2)
    val key = parts[0].trim()
    val value = parts[1].trim()

    if (key.isBlank() || value.isBlank()) {
        return null
    }

    return key to value
}

private fun parseInvariant(input: String): Invariant? {
    val parts = input.split("|", limit = 3)

    if (parts.size < 2) {
        return null
    }

    val id = parts[0].trim()
    val description = parts[1].trim()
    val forbiddenKeywords = parts
        .getOrNull(2)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    if (id.isBlank() || description.isBlank()) {
        return null
    }

    return Invariant(
        id = id,
        description = description,
        forbiddenKeywords = forbiddenKeywords
    )
}

private fun parseTaskStage(input: String): TaskStage? {
    return when (input.lowercase()) {
        "planning" -> TaskStage.PLANNING
        "execution" -> TaskStage.EXECUTION
        "validation" -> TaskStage.VALIDATION
        "done" -> TaskStage.DONE
        else -> null
    }
}

private fun String.removePrefixIgnoreCase(prefix: String): String {
    return if (startsWith(prefix, ignoreCase = true)) {
        substring(prefix.length)
    } else {
        this
    }
}
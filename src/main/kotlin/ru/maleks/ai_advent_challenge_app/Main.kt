package ru.maleks.ai_advent_challenge_app

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import ru.maleks.ai_advent_challenge_app.agent.LayeredMemoryAgent
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.memory.AssistantMemoryStorage
import ru.maleks.ai_advent_challenge_app.profile.UserProfileStorage
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

    val profiles = profileStorage.loadProfiles()
    var activeProfile = profiles["backend"] ?: profiles.values.first()

    val agent = LayeredMemoryAgent(
        name = "LayeredMemoryAgent",
        llmClient = llmClient,
        memoryStorage = memoryStorage,
        userProfile = activeProfile,
        taskStateMachine = taskStateMachine
    )

    println("AI Advent Challenge — Day 13")
    println("Agent: ${agent.name}")
    println("Model: $model")
    println()
    println("Commands:")
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
    println("  reset-state")
    println("  exit")
    println()

    while (true) {
        print("You: ")
        val input = readlnOrNull()?.trim()

        if (input.isNullOrBlank()) {
            continue
        }

        when {
            input.equals("exit", ignoreCase = true) -> break

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
                val moved = taskStateMachine.next()
                if (moved) {
                    println("Moved to stage: ${taskStateMachine.current().stage}")
                } else {
                    println("Already at final stage: ${taskStateMachine.current().stage}")
                }
                continue
            }

            input.equals("back", ignoreCase = true) -> {
                val moved = taskStateMachine.back()
                if (moved) {
                    println("Moved back to stage: ${taskStateMachine.current().stage}")
                } else {
                    println("Already at first stage: ${taskStateMachine.current().stage}")
                }
                continue
            }

            input.equals("reset-state", ignoreCase = true) -> {
                taskStateMachine.reset()
                println("Task state reset.")
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

    httpClient.close()
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

private fun String.removePrefixIgnoreCase(prefix: String): String {
    return if (startsWith(prefix, ignoreCase = true)) {
        substring(prefix.length)
    } else {
        this
    }
}
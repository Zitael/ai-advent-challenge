package ru.maleks.ai_advent_challenge_app.agent

import ru.maleks.ai_advent_challenge_app.invariant.InvariantChecker
import ru.maleks.ai_advent_challenge_app.invariant.InvariantViolation
import ru.maleks.ai_advent_challenge_app.llm.LlmClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.memory.AssistantMemory
import ru.maleks.ai_advent_challenge_app.memory.AssistantMemoryStorage
import ru.maleks.ai_advent_challenge_app.profile.UserProfile
import ru.maleks.ai_advent_challenge_app.state.TaskStateMachine

class LayeredMemoryAgent(
    override val name: String,
    private val llmClient: LlmClient,
    private val memoryStorage: AssistantMemoryStorage,
    private var userProfile: UserProfile,
    private val taskStateMachine: TaskStateMachine,
    private val invariantChecker: InvariantChecker,
    private val keepLastShortTermMessages: Int = 8
) : Agent {

    private var memory: AssistantMemory = memoryStorage.load()

    override suspend fun handle(userInput: String): String {
        val violations = invariantChecker.check(userInput)

        if (violations.isNotEmpty()) {
            val refusal = buildInvariantRefusal(violations)

            memory.shortTerm.add(OpenRouterMessage(role = "user", content = userInput))
            memory.shortTerm.add(OpenRouterMessage(role = "assistant", content = refusal))
            trimShortTerm()
            memoryStorage.save(memory)

            return refusal
        }

        val requestMessages = buildContext(userInput)

        val result = llmClient.complete(requestMessages)

        memory.shortTerm.add(OpenRouterMessage(role = "user", content = userInput))
        memory.shortTerm.add(OpenRouterMessage(role = "assistant", content = result.answer))

        trimShortTerm()
        memoryStorage.save(memory)

        println()
        println("----- AGENT STATS -----")
        println("Active profile: ${userProfile.id} — ${userProfile.name}")
        println("Task stage: ${taskStateMachine.current().stage}")
        println("Invariants: ${invariantChecker.getAll().size}")
        println("Short-term messages: ${memory.shortTerm.size}")
        println("Working memory items: ${memory.working.size}")
        println("Long-term memory items: ${memory.longTerm.size}")
        println("API prompt tokens: ${result.usage?.promptTokens}")
        println("API completion tokens: ${result.usage?.completionTokens}")
        println("API total tokens: ${result.usage?.totalTokens}")
        println("API cost: ${result.usage?.cost}")
        println("-----------------------")
        println()

        return result.answer
    }

    fun setUserProfile(profile: UserProfile) {
        userProfile = profile
    }

    fun rememberShort(text: String) {
        memory.shortTerm.add(OpenRouterMessage(role = "user", content = text))
        trimShortTerm()
        memoryStorage.save(memory)
    }

    fun rememberWorking(key: String, value: String) {
        memory.working[key] = value
        memoryStorage.save(memory)
    }

    fun rememberLongTerm(key: String, value: String) {
        memory.longTerm[key] = value
        memoryStorage.save(memory)
    }

    fun printMemory() {
        println()
        println("========== ACTIVE PROFILE ==========")
        println("- id: ${userProfile.id}")
        println("- name: ${userProfile.name}")
        println("- style: ${userProfile.style}")
        println("- format: ${userProfile.format}")
        println("- context: ${userProfile.context}")
        println("- constraints:")
        userProfile.constraints.forEach { println("  - $it") }

        println()
        println("========== MEMORY ==========")

        println()
        println("SHORT-TERM MEMORY:")
        if (memory.shortTerm.isEmpty()) {
            println("empty")
        } else {
            memory.shortTerm.forEachIndexed { index, message ->
                println("${index + 1}. ${message.role}: ${message.content}")
            }
        }

        println()
        println("WORKING MEMORY:")
        printMap(memory.working)

        println()
        println("LONG-TERM MEMORY:")
        printMap(memory.longTerm)

        println()
        println("============================")
        println()
    }

    fun clearMemory() {
        memory = AssistantMemory()
        memoryStorage.save(memory)
    }

    private fun buildContext(userInput: String): List<OpenRouterMessage> {
        val taskState = taskStateMachine.current()

        val result = mutableListOf<OpenRouterMessage>()

        result.add(
            OpenRouterMessage(
                role = "system",
                content = """
                    You are a stateful AI assistant with explicit layered memory, user personalization, task state machine and invariants.

                    Memory layers:
                    1. Short-term memory — recent dialogue.
                    2. Working memory — current task data, goals, constraints, decisions.
                    3. Long-term memory — stable user profile, preferences, stack and reusable knowledge.

                    Task state machine:
                    - PLANNING: clarify task and create a plan.
                    - EXECUTION: perform the approved plan.
                    - VALIDATION: check result and find issues.
                    - DONE: summarize completed work.

                    Invariant rules:
                    - Invariants are hard constraints.
                    - Never propose solutions that violate invariants.
                    - If the user asks for something that conflicts with invariants, refuse briefly and suggest an allowed alternative.

                    General rules:
                    - Use active user profile for personalization.
                    - Use long-term memory for stable context.
                    - Use working memory for the current task.
                    - Use short-term memory for recent dialogue.
                    - Use task state to decide what kind of answer is expected now.
                    - Do not jump to another task stage unless user explicitly changes it.
                    - Do not invent memory facts that were not provided.
                """.trimIndent()
            )
        )

        result.add(
            OpenRouterMessage(
                role = "system",
                content = """
                    Active user profile:
                    - id: ${userProfile.id}
                    - name: ${userProfile.name}
                    - style: ${userProfile.style}
                    - response format: ${userProfile.format}
                    - context: ${userProfile.context}
                    - constraints:
                    ${userProfile.constraints.joinToString("\n") { "  - $it" }}

                    Adapt every answer to this profile automatically.
                """.trimIndent()
            )
        )

        result.add(
            OpenRouterMessage(
                role = "system",
                content = """
                    Current task state:
                    - stage: ${taskState.stage}
                    - task description: ${taskState.taskDescription.ifBlank { "not set" }}
                    - current step: ${taskState.currentStep}
                    - expected action: ${taskState.expectedAction}
                    - approved plan:
                    ${taskState.approvedPlan.ifBlank { "not set" }}

                    Follow the current task stage.
                """.trimIndent()
            )
        )

        result.add(
            OpenRouterMessage(
                role = "system",
                content = """
                    Project invariants:
                    ${invariantChecker.formatForPrompt()}

                    These invariants must not be violated.
                """.trimIndent()
            )
        )

        if (memory.longTerm.isNotEmpty()) {
            result.add(
                OpenRouterMessage(
                    role = "system",
                    content = """
                        Long-term memory:
                        ${formatMap(memory.longTerm)}
                    """.trimIndent()
                )
            )
        }

        if (memory.working.isNotEmpty()) {
            result.add(
                OpenRouterMessage(
                    role = "system",
                    content = """
                        Working memory:
                        ${formatMap(memory.working)}
                    """.trimIndent()
                )
            )
        }

        if (memory.shortTerm.isNotEmpty()) {
            result.add(
                OpenRouterMessage(
                    role = "system",
                    content = "Short-term memory contains recent dialogue messages below."
                )
            )
            result.addAll(memory.shortTerm.takeLast(keepLastShortTermMessages))
        }

        result.add(OpenRouterMessage(role = "user", content = userInput))

        return result
    }

    private fun buildInvariantRefusal(violations: List<InvariantViolation>): String {
        val details = violations.joinToString("\n") { violation ->
            """
            - ${violation.invariantId}: ${violation.invariantDescription}
              matched forbidden keywords: ${violation.matchedKeywords.joinToString(", ")}
            """.trimIndent()
        }

        return """
            I cannot follow this request because it violates configured invariants:

            $details

            Please reformulate the request within the accepted architecture, stack and project constraints.
        """.trimIndent()
    }

    private fun trimShortTerm() {
        while (memory.shortTerm.size > keepLastShortTermMessages) {
            memory.shortTerm.removeAt(0)
        }
    }

    private fun formatMap(map: Map<String, String>): String {
        return map.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
    }

    private fun printMap(map: Map<String, String>) {
        if (map.isEmpty()) {
            println("empty")
            return
        }

        map.forEach { (key, value) ->
            println("- $key: $value")
        }
    }
}
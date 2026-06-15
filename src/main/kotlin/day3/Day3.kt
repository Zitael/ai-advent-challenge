package day3

import api.LLMApiClient
import api.OpenRouterMessage
import api.OpenRouterRequest
import io.ktor.client.HttpClient
import ru.ai_advent_app.day1.day3.ReasoningMode
import ru.ai_advent_app.day1.day3.buildPrompt

class Day3 {

    suspend fun run(model: String, apiKey: String) {

        val api = LLMApiClient(apiKey)

        println("AI Advent Challenge — Day 3")
        println("Model: $model")

        val task = """
            You have 8 coins. One coin is fake and lighter than the others.
            You have a balance scale without weights.
            What is the minimum number of weighings needed to find the fake coin?
        """.trimIndent()

        println("Reasoning, task: $task")
        println()

        try {
            val task = """
                You have 8 coins. One coin is fake and lighter than the others.
                You have a balance scale without weights.
                What is the minimum number of weighings needed to find the fake coin?
            """.trimIndent()

            val directAnswer = api.send(prepareRequest(buildPrompt(ReasoningMode.DIRECT, task), model))

            val stepByStepAnswer = api.send(prepareRequest(buildPrompt(ReasoningMode.STEP_BY_STEP, task), model))

            val promptGenerationTask = """
                Create a high-quality prompt for solving this logic task.
                The prompt should force careful reasoning and a concise final answer.
    
                Task:
                $task
            """.trimIndent()

            val generatedPrompt = api.send(prepareRequest(promptGenerationTask, model)).choices
                .firstOrNull()
                ?.message
                ?.content
                ?: "No content in response"

            val generatedPromptAnswer = api.send(
                prepareRequest(
                    buildPrompt(
                        mode = ReasoningMode.GENERATED_PROMPT,
                        task = task,
                        generatedPrompt = generatedPrompt
                    ),
                    model
                )
            )

            val expertGroupAnswer = api.send(prepareRequest(buildPrompt(ReasoningMode.EXPERT_GROUP, task), model))

            println("TASK:")
            println(task)

            println("\n================ DIRECT ================")
            println(directAnswer)

            println("\n================ STEP BY STEP ================")
            println(stepByStepAnswer)

            println("\n================ GENERATED PROMPT ================")
            println("Generated prompt:")
            println(generatedPrompt)
            println("\nAnswer:")
            println(generatedPromptAnswer)

            println("\n================ EXPERT GROUP ================")
            println(expertGroupAnswer)

            println("\n================ COMPARISON ================")
            println("""
    Expected answer: 2 weighings.
    
    Compare:
    - Did the answer contain the correct number?
    - Did it explain the weighing strategy?
    - Was the answer concise?
    - Did the expert group find mistakes or just repeat itself?
""".trimIndent())


        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    fun prepareRequest(
        prompt: String,
        model: String,
        maxTokens: Int? = null,
        stop: List<String>? = null,
    ): OpenRouterRequest {
        return OpenRouterRequest(
            model,
            listOf(OpenRouterMessage(role = "user", content = prompt)),
            maxTokens,
            stop
        )
    }
}
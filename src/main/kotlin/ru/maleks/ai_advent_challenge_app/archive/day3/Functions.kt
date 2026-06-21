package ru.maleks.ai_advent_challenge_app.archive.day3

enum class ReasoningMode {
    DIRECT,
    STEP_BY_STEP,
    GENERATED_PROMPT,
    EXPERT_GROUP
}

fun buildPrompt(mode: ReasoningMode, task: String, generatedPrompt: String? = null): String {
    return when (mode) {
        ReasoningMode.DIRECT -> task

        ReasoningMode.STEP_BY_STEP -> """
            Solve the task step by step.
            
            Task:
            $task
        """.trimIndent()

        ReasoningMode.GENERATED_PROMPT -> generatedPrompt
            ?: error("Generated prompt is required for GENERATED_PROMPT mode")

        ReasoningMode.EXPERT_GROUP -> """
            Solve the task using a group of experts.
            
            Experts:
            1. Logical analyst
            2. Algorithm engineer
            3. Critical reviewer
            
            Each expert should provide their solution.
            Then provide the final answer.
            
            Task:
            $task
        """.trimIndent()
    }
}
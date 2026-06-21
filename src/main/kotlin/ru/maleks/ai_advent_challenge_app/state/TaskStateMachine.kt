package ru.maleks.ai_advent_challenge_app.state

class TaskStateMachine(
    private val storage: TaskStateStorage
) {
    private var state: TaskState = storage.load()

    private val allowedTransitions: Map<TaskStage, Set<TaskStage>> = mapOf(
        TaskStage.PLANNING to setOf(TaskStage.EXECUTION),
        TaskStage.EXECUTION to setOf(TaskStage.PLANNING, TaskStage.VALIDATION),
        TaskStage.VALIDATION to setOf(TaskStage.EXECUTION, TaskStage.DONE),
        TaskStage.DONE to setOf(TaskStage.VALIDATION)
    )

    fun current(): TaskState = state

    fun setTask(description: String) {
        state = state.copy(
            stage = TaskStage.PLANNING,
            taskDescription = description,
            currentStep = "Create and approve implementation plan",
            expectedAction = "Assistant should create a plan. User should approve or refine it.",
            approvedPlan = ""
        )
        storage.save(state)
    }

    fun setCurrentStep(step: String) {
        state = state.copy(currentStep = step)
        storage.save(state)
    }

    fun setExpectedAction(action: String) {
        state = state.copy(expectedAction = action)
        storage.save(state)
    }

    fun setApprovedPlan(plan: String) {
        state = state.copy(
            approvedPlan = plan,
            currentStep = "Execute approved plan",
            expectedAction = "Assistant should implement or describe the next execution step"
        )
        storage.save(state)
    }

    fun transitionTo(targetStage: TaskStage): TaskTransitionResult {
        val currentStage = state.stage

        if (currentStage == targetStage) {
            return TaskTransitionResult(
                success = true,
                message = "Already at stage: $targetStage",
                currentStage = currentStage
            )
        }

        val allowedTargets = allowedTransitions[currentStage].orEmpty()

        if (!allowedTargets.contains(targetStage)) {
            return TaskTransitionResult(
                success = false,
                message = """
                    Invalid transition: $currentStage -> $targetStage
                    
                    Allowed transitions from $currentStage:
                    ${allowedTargets.joinToString(", ")}
                """.trimIndent(),
                currentStage = currentStage
            )
        }

        state = state.copy(
            stage = targetStage,
            currentStep = defaultStepFor(targetStage),
            expectedAction = defaultExpectedActionFor(targetStage)
        )

        storage.save(state)

        return TaskTransitionResult(
            success = true,
            message = "Moved from $currentStage to $targetStage",
            currentStage = targetStage
        )
    }

    fun next(): TaskTransitionResult {
        val targetStage = when (state.stage) {
            TaskStage.PLANNING -> TaskStage.EXECUTION
            TaskStage.EXECUTION -> TaskStage.VALIDATION
            TaskStage.VALIDATION -> TaskStage.DONE
            TaskStage.DONE -> return TaskTransitionResult(
                success = false,
                message = "Already at final stage: DONE",
                currentStage = TaskStage.DONE
            )
        }

        return transitionTo(targetStage)
    }

    fun back(): TaskTransitionResult {
        val targetStage = when (state.stage) {
            TaskStage.PLANNING -> return TaskTransitionResult(
                success = false,
                message = "Already at first stage: PLANNING",
                currentStage = TaskStage.PLANNING
            )

            TaskStage.EXECUTION -> TaskStage.PLANNING
            TaskStage.VALIDATION -> TaskStage.EXECUTION
            TaskStage.DONE -> TaskStage.VALIDATION
        }

        return transitionTo(targetStage)
    }

    fun reset() {
        state = TaskState()
        storage.save(state)
    }

    fun printState() {
        println()
        println("========== TASK STATE ==========")
        println("Stage: ${state.stage}")
        println("Task: ${state.taskDescription.ifBlank { "not set" }}")
        println("Current step: ${state.currentStep}")
        println("Expected action: ${state.expectedAction}")
        println("Approved plan:")
        println(state.approvedPlan.ifBlank { "not set" })
        println()
        println("Allowed next stages:")
        println(allowedTransitions[state.stage].orEmpty().joinToString(", "))
        println("================================")
        println()
    }

    fun formatForPrompt(): String {
        val allowedTargets = allowedTransitions[state.stage].orEmpty()

        return """
            Current task state:
            - stage: ${state.stage}
            - task description: ${state.taskDescription.ifBlank { "not set" }}
            - current step: ${state.currentStep}
            - expected action: ${state.expectedAction}
            - approved plan:
            ${state.approvedPlan.ifBlank { "not set" }}

            Allowed transitions from current stage:
            ${allowedTargets.joinToString(", ").ifBlank { "none" }}

            Lifecycle rules:
            - Do not execute implementation before PLANNING is completed.
            - Do not move to DONE before VALIDATION.
            - Do not skip stages.
            - If user asks to skip required stage, explain that transition is not allowed.
        """.trimIndent()
    }

    private fun defaultStepFor(stage: TaskStage): String {
        return when (stage) {
            TaskStage.PLANNING -> "Create and approve implementation plan"
            TaskStage.EXECUTION -> "Execute approved plan"
            TaskStage.VALIDATION -> "Validate result and check whether task is complete"
            TaskStage.DONE -> "Task is completed"
        }
    }

    private fun defaultExpectedActionFor(stage: TaskStage): String {
        return when (stage) {
            TaskStage.PLANNING -> "Assistant should create a plan. User should approve or refine it."
            TaskStage.EXECUTION -> "Assistant should implement or describe the next execution step."
            TaskStage.VALIDATION -> "Assistant should validate the result and find possible issues."
            TaskStage.DONE -> "Assistant should summarize the completed task."
        }
    }
}
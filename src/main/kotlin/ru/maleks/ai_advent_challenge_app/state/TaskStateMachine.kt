package ru.maleks.ai_advent_challenge_app.state

class TaskStateMachine(
    private val storage: TaskStateStorage
) {
    private var state: TaskState = storage.load()

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
        state = state.copy(
            currentStep = step
        )
        storage.save(state)
    }

    fun setExpectedAction(action: String) {
        state = state.copy(
            expectedAction = action
        )
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

    fun next(): Boolean {
        val nextStage = when (state.stage) {
            TaskStage.PLANNING -> TaskStage.EXECUTION
            TaskStage.EXECUTION -> TaskStage.VALIDATION
            TaskStage.VALIDATION -> TaskStage.DONE
            TaskStage.DONE -> return false
        }

        state = state.copy(
            stage = nextStage,
            currentStep = defaultStepFor(nextStage),
            expectedAction = defaultExpectedActionFor(nextStage)
        )

        storage.save(state)
        return true
    }

    fun back(): Boolean {
        val previousStage = when (state.stage) {
            TaskStage.PLANNING -> return false
            TaskStage.EXECUTION -> TaskStage.PLANNING
            TaskStage.VALIDATION -> TaskStage.EXECUTION
            TaskStage.DONE -> TaskStage.VALIDATION
        }

        state = state.copy(
            stage = previousStage,
            currentStep = defaultStepFor(previousStage),
            expectedAction = defaultExpectedActionFor(previousStage)
        )

        storage.save(state)
        return true
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
        println("================================")
        println()
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
package ru.maleks.ai_advent_challenge_app.state

data class TaskState(
    val stage: TaskStage = TaskStage.PLANNING,
    val taskDescription: String = "",
    val currentStep: String = "Collect requirements and create a plan",
    val expectedAction: String = "User should describe the task or confirm the plan",
    val approvedPlan: String = ""
)
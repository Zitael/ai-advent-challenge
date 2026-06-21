package ru.maleks.ai_advent_challenge_app.state

data class TaskTransitionResult(
    val success: Boolean,
    val message: String,
    val currentStage: TaskStage
)
package ru.maleks.ai_advent_challenge_app.mcp.server.scheduler

data class TaskSummarySnapshot(
    val collectedAt: String,
    val totalTasks: Int,
    val todoTasks: Int,
    val inProgressTasks: Int,
    val doneTasks: Int,
    val highPriorityTasks: Int,
    val summary: String
)
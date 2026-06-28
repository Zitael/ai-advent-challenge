package ru.maleks.ai_advent_challenge_app.mcp.server.scheduler

data class TaskSummaryState(
    val snapshots: MutableList<TaskSummarySnapshot> = mutableListOf()
)
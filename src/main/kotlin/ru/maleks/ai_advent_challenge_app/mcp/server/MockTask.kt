package ru.maleks.ai_advent_challenge_app.mcp.server

data class MockTask(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    val assignee: String,
    val description: String
)
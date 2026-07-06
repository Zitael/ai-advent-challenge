package ru.maleks.ai_advent_challenge_app.rag.qa

data class ControlQuestion(
    val id: Int,
    val question: String,
    val expectedAnswerShouldContain: List<String>,
    val expectedSources: List<String>
)
package ru.maleks.ai_advent_challenge_app.rag.chat

data class RagTaskMemory(
    var goal: String = "",
    val fixedTerms: MutableMap<String, String> = mutableMapOf(),
    val constraints: MutableList<String> = mutableListOf(),
    val userClarifications: MutableList<String> = mutableListOf()
)
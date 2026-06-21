package ru.maleks.ai_advent_challenge_app.memory

import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage

data class AssistantMemory(
    val shortTerm: MutableList<OpenRouterMessage> = mutableListOf(),
    val working: MutableMap<String, String> = mutableMapOf(),
    val longTerm: MutableMap<String, String> = mutableMapOf()
)
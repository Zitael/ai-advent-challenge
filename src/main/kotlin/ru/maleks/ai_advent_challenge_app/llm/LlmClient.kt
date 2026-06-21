package ru.maleks.ai_advent_challenge_app.llm

interface LlmClient {
    suspend fun complete(messages: List<OpenRouterMessage>): LlmResult
}
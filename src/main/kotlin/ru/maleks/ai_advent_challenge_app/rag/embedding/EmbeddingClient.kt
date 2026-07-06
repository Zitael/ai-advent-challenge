package ru.maleks.ai_advent_challenge_app.rag.embedding

interface EmbeddingClient {
    val modelName: String

    fun embed(text: String): List<Double>
}
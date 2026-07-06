package ru.maleks.ai_advent_challenge_app.rag.model

data class IndexStats(
    val documents: Int,
    val chunks: Int,
    val totalWords: Int,
    val averageChunkWords: Double,
    val minChunkWords: Int,
    val maxChunkWords: Int
)
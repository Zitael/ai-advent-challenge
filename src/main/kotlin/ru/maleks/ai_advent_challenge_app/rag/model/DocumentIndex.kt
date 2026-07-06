package ru.maleks.ai_advent_challenge_app.rag.model

data class DocumentIndex(
    val strategy: String,
    val createdAt: String,
    val embeddingModel: String,
    val sourceDirectory: String,
    val stats: IndexStats,
    val chunks: List<DocumentChunk>
)
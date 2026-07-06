package ru.maleks.ai_advent_challenge_app.rag.model

data class DocumentChunk(
    val chunkId: String,
    val source: String,
    val title: String,
    val section: String,
    val strategy: String,
    val text: String,
    val startWord: Int,
    val endWord: Int,
    val embedding: List<Double> = emptyList()
)
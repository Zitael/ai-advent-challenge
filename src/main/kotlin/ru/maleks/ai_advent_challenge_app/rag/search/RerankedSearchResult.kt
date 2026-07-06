package ru.maleks.ai_advent_challenge_app.rag.search

import ru.maleks.ai_advent_challenge_app.rag.model.DocumentChunk

data class RerankedSearchResult(
    val chunk: DocumentChunk,
    val similarityScore: Double,
    val keywordScore: Double,
    val finalScore: Double
)
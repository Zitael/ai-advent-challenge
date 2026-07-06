package ru.maleks.ai_advent_challenge_app.rag.search

import ru.maleks.ai_advent_challenge_app.rag.model.DocumentChunk

data class SearchResult(
    val chunk: DocumentChunk,
    val score: Double
)
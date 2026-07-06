package ru.maleks.ai_advent_challenge_app.rag.chunk

import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentChunk

interface ChunkingStrategy {
    val name: String

    fun chunk(documents: List<RawDocument>): List<DocumentChunk>
}
package ru.maleks.ai_advent_challenge_app.rag.index

import ru.maleks.ai_advent_challenge_app.rag.chunk.ChunkingStrategy
import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import ru.maleks.ai_advent_challenge_app.rag.embedding.EmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentChunk
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.model.IndexStats
import java.time.Instant

class DocumentIndexer(
    private val embeddingClient: EmbeddingClient
) {

    fun buildIndex(
        documents: List<RawDocument>,
        strategy: ChunkingStrategy,
        sourceDirectory: String
    ): DocumentIndex {
        val rawChunks = strategy.chunk(documents)

        val embeddedChunks = rawChunks.mapIndexed { index, chunk ->
            if ((index + 1) % 10 == 0 || index == rawChunks.lastIndex) {
                println("Embedding ${index + 1}/${rawChunks.size} for strategy '${strategy.name}'")
            }

            chunk.copy(
                embedding = embeddingClient.embed(chunk.text)
            )
        }

        return DocumentIndex(
            strategy = strategy.name,
            createdAt = Instant.now().toString(),
            embeddingModel = embeddingClient.modelName,
            sourceDirectory = sourceDirectory,
            stats = buildStats(documents, embeddedChunks),
            chunks = embeddedChunks
        )
    }

    private fun buildStats(
        documents: List<RawDocument>,
        chunks: List<DocumentChunk>
    ): IndexStats {
        val chunkWordCounts = chunks.map { it.text.wordCount() }

        return IndexStats(
            documents = documents.size,
            chunks = chunks.size,
            totalWords = documents.sumOf { it.text.wordCount() },
            averageChunkWords = if (chunkWordCounts.isEmpty()) 0.0 else chunkWordCounts.average(),
            minChunkWords = chunkWordCounts.minOrNull() ?: 0,
            maxChunkWords = chunkWordCounts.maxOrNull() ?: 0
        )
    }

    private fun String.wordCount(): Int {
        return split(Regex("\\s+"))
            .count { it.isNotBlank() }
    }
}
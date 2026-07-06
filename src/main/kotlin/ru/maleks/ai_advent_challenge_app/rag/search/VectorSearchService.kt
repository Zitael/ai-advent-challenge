package ru.maleks.ai_advent_challenge_app.rag.search

import ru.maleks.ai_advent_challenge_app.rag.embedding.EmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import kotlin.math.sqrt

class VectorSearchService(
    private val embeddingClient: EmbeddingClient
) {

    fun search(
        question: String,
        index: DocumentIndex,
        topK: Int = 3
    ): List<SearchResult> {
        val questionEmbedding = embeddingClient.embed(question)

        return index.chunks
            .map { chunk ->
                SearchResult(
                    chunk = chunk,
                    score = cosineSimilarity(questionEmbedding, chunk.embedding)
                )
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun cosineSimilarity(first: List<Double>, second: List<Double>): Double {
        if (first.isEmpty() || second.isEmpty() || first.size != second.size) {
            return 0.0
        }

        val dot = first.indices.sumOf { i -> first[i] * second[i] }
        val firstNorm = sqrt(first.sumOf { it * it })
        val secondNorm = sqrt(second.sumOf { it * it })

        if (firstNorm == 0.0 || secondNorm == 0.0) {
            return 0.0
        }

        return dot / (firstNorm * secondNorm)
    }
}
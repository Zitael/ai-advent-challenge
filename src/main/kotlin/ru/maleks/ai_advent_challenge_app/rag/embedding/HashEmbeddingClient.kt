package ru.maleks.ai_advent_challenge_app.rag.embedding

import kotlin.math.sqrt

class HashEmbeddingClient(
    private val dimension: Int = 384
) : EmbeddingClient {

    override val modelName: String = "local-hash-embedding-$dimension"

    override fun embed(text: String): List<Double> {
        val vector = DoubleArray(dimension)

        tokenize(text).forEach { token ->
            val hash = token.hashCode()
            val index = (hash and Int.MAX_VALUE) % dimension
            val sign = if (hash % 2 == 0) 1.0 else -1.0
            vector[index] += sign
        }

        normalize(vector)

        return vector.toList()
    }

    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .split(Regex("[^a-zа-яё0-9_]+"))
            .map { it.trim() }
            .filter { it.length >= 3 }
    }

    private fun normalize(vector: DoubleArray) {
        val norm = sqrt(vector.sumOf { it * it })

        if (norm == 0.0) {
            return
        }

        for (i in vector.indices) {
            vector[i] = vector[i] / norm
        }
    }
}
package ru.maleks.ai_advent_challenge_app.rag.search

import kotlin.math.ln

class HeuristicReranker {

    fun rerank(
        originalQuestion: String,
        results: List<SearchResult>
    ): List<RerankedSearchResult> {
        val queryTokens = tokenize(originalQuestion).toSet()

        return results
            .map { result ->
                val chunkTokens = tokenize(result.chunk.text).toSet()
                val keywordMatches = queryTokens.count { it in chunkTokens }

                val keywordScore = if (queryTokens.isEmpty()) {
                    0.0
                } else {
                    keywordMatches.toDouble() / queryTokens.size
                }

                val sectionBoost = if (
                    queryTokens.any { token ->
                        result.chunk.section.lowercase().contains(token) ||
                                result.chunk.title.lowercase().contains(token)
                    }
                ) {
                    0.08
                } else {
                    0.0
                }

                val lengthPenalty = when {
                    result.chunk.text.length < 200 -> 0.03
                    result.chunk.text.length > 3_000 -> 0.04
                    else -> 0.0
                }

                val finalScore =
                    result.score * 0.70 +
                            keywordScore * 0.25 +
                            sectionBoost -
                            lengthPenalty

                RerankedSearchResult(
                    chunk = result.chunk,
                    similarityScore = result.score,
                    keywordScore = keywordScore,
                    finalScore = finalScore
                )
            }
            .sortedByDescending { it.finalScore }
    }

    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .split(Regex("[^a-zа-яё0-9_]+"))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .map { normalizeToken(it) }
            .filter { it.isNotBlank() }
    }

    private fun normalizeToken(token: String): String {
        return token
            .replace("ё", "е")
            .trim()
    }
}
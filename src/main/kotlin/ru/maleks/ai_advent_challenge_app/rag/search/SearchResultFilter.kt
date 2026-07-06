package ru.maleks.ai_advent_challenge_app.rag.search

class SearchResultFilter(
    private val minSimilarity: Double = 0.08
) {

    fun filter(results: List<SearchResult>): List<SearchResult> {
        return results.filter { it.score >= minSimilarity }
    }
}
package ru.maleks.ai_advent_challenge_app.rag.search

data class ImprovedRetrieveResult(
    val originalQuestion: String,
    val rewrittenQuery: String,
    val rawResults: List<SearchResult>,
    val filteredResults: List<SearchResult>,
    val rerankedResults: List<RerankedSearchResult>
)
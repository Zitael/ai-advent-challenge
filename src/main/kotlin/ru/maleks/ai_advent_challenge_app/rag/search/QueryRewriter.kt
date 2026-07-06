package ru.maleks.ai_advent_challenge_app.rag.search

class QueryRewriter {

    fun rewrite(question: String): String {
        val expansions = mapOf(
            "мсп" to "MCP Model Context Protocol tools tool server client",
            "mcp" to "MCP Model Context Protocol tools tool server client",
            "раг" to "RAG Retrieval Augmented Generation embedding chunking vector search",
            "rag" to "RAG Retrieval Augmented Generation embedding chunking vector search",
            "эмбеддинг" to "embedding vector embeddings вектор",
            "чанк" to "chunk chunking разбиение раздел",
            "агент" to "AI agent runtime memory profile state machine invariants tools",
            "память" to "memory short-term working long-term profile context",
            "стейт" to "state machine planning execution validation done",
            "redis" to "Redis Redisson cache caching distributed lock"
        )

        val lower = question.lowercase()
        val extraTerms = expansions
            .filterKeys { key -> lower.contains(key) }
            .values
            .distinct()
            .joinToString(" ")

        return if (extraTerms.isBlank()) {
            question
        } else {
            "$question $extraTerms"
        }
    }
}
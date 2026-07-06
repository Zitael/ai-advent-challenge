package ru.maleks.ai_advent_challenge_app.rag.prompt

import ru.maleks.ai_advent_challenge_app.rag.search.RerankedSearchResult
import ru.maleks.ai_advent_challenge_app.rag.search.SearchResult

class RagPromptBuilder {

    fun build(question: String, results: List<SearchResult>): String {
        val context = results.joinToString("\n\n") { result ->
            """
            Source: ${result.chunk.source}
            Title: ${result.chunk.title}
            Section: ${result.chunk.section}
            Similarity score: ${"%.4f".format(result.score)}

            ${result.chunk.text}
            """.trimIndent()
        }

        return buildPrompt(question, context)
    }

    fun buildFromReranked(question: String, results: List<RerankedSearchResult>): String {
        val context = results.joinToString("\n\n") { result ->
            """
            Source: ${result.chunk.source}
            Title: ${result.chunk.title}
            Section: ${result.chunk.section}
            Similarity score: ${"%.4f".format(result.similarityScore)}
            Keyword score: ${"%.4f".format(result.keywordScore)}
            Final rerank score: ${"%.4f".format(result.finalScore)}

            ${result.chunk.text}
            """.trimIndent()
        }

        return buildPrompt(question, context)
    }

    private fun buildPrompt(question: String, context: String): String {
        return """
            Ты отвечаешь на вопрос пользователя, используя только контекст ниже.

            Правила:
            - Если ответа нет в контексте, так и скажи.
            - Не выдумывай факты.
            - В конце перечисли использованные источники.
            - Отвечай кратко и по делу.

            Контекст:
            $context

            Вопрос пользователя:
            $question
        """.trimIndent()
    }
}
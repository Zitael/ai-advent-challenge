package ru.maleks.ai_advent_challenge_app.rag.prompt

import ru.maleks.ai_advent_challenge_app.rag.search.SearchResult

class RagPromptBuilder {

    fun build(question: String, results: List<SearchResult>): String {
        val context = results.joinToString("\n\n") { result ->
            """
            Source: ${result.chunk.source}
            Title: ${result.chunk.title}
            Section: ${result.chunk.section}
            Score: ${"%.4f".format(result.score)}

            ${result.chunk.text}
            """.trimIndent()
        }

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
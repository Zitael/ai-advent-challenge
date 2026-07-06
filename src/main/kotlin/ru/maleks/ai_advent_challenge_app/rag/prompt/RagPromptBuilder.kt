package ru.maleks.ai_advent_challenge_app.rag.prompt

import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContext
import ru.maleks.ai_advent_challenge_app.rag.search.RerankedSearchResult
import ru.maleks.ai_advent_challenge_app.rag.search.SearchResult

class RagPromptBuilder {

    fun build(question: String, results: List<SearchResult>): String {
        val context = results.joinToString("\n\n") { result ->
            """
            Source: ${result.chunk.source}
            Title: ${result.chunk.title}
            Section: ${result.chunk.section}
            Chunk ID: ${result.chunk.chunkId}
            Similarity score: ${"%.4f".format(result.score)}

            ${result.chunk.text}
            """.trimIndent()
        }

        return buildSimplePrompt(question, context)
    }

    fun buildFromReranked(question: String, results: List<RerankedSearchResult>): String {
        val context = results.joinToString("\n\n") { result ->
            """
            Source: ${result.chunk.source}
            Title: ${result.chunk.title}
            Section: ${result.chunk.section}
            Chunk ID: ${result.chunk.chunkId}
            Similarity score: ${"%.4f".format(result.similarityScore)}
            Keyword score: ${"%.4f".format(result.keywordScore)}
            Final rerank score: ${"%.4f".format(result.finalScore)}

            ${result.chunk.text}
            """.trimIndent()
        }

        return buildSimplePrompt(question, context)
    }

    fun buildGroundedPrompt(context: GroundedRagContext): String {
        val sourcesText = context.sources.joinToString("\n\n") { source ->
            """
            [SOURCE]
            source: ${source.source}
            title: ${source.title}
            section: ${source.section}
            chunk_id: ${source.chunkId}
            score: ${"%.4f".format(source.score)}
            quote: "${source.quote}"
            [/SOURCE]
            """.trimIndent()
        }

        return """
            Ты отвечаешь на вопрос пользователя только по переданным источникам.

            Обязательный формат ответа:

            ## Ответ
            Краткий ответ на вопрос.

            ## Источники
            - source: ...
              section: ...
              chunk_id: ...

            ## Цитаты
            - "точная цитата из источника"

            Жёсткие правила:
            - Используй только переданные источники.
            - Не добавляй факты, которых нет в источниках.
            - Каждый ответ обязан содержать блок "Источники".
            - Каждый ответ обязан содержать блок "Цитаты".
            - Цитаты должны быть взяты из поля quote.
            - Если источников недостаточно, скажи: "Не знаю по имеющейся базе знаний. Нужно уточнение."

            Источники:
            $sourcesText

            Вопрос пользователя:
            ${context.question}
        """.trimIndent()
    }

    private fun buildSimplePrompt(question: String, context: String): String {
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
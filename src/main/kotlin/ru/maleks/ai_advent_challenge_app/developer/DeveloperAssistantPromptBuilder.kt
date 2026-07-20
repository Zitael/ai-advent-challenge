package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContext

class DeveloperAssistantPromptBuilder {

    fun build(
        question: String,
        branch: String,
        groundedContext: GroundedRagContext
    ): String {
        val sources = groundedContext.sources.joinToString("\n\n") { source ->
            """
            [SOURCE]
            file: ${source.source}
            section: ${source.section}
            chunk_id: ${source.chunkId}
            relevance: ${"%.4f".format(source.score)}
            content: ${source.quote}
            [/SOURCE]
            """.trimIndent()
        }

        return """
            Ты ассистент разработчика, подключённый к текущему проекту.

            Текущая git-ветка:
            $branch

            Отвечай только на основании документации проекта, переданной ниже.
            Не выдумывай классы, API, команды, зависимости и архитектурные решения.
            Если документации недостаточно, прямо скажи об этом и укажи, чего не хватает.
            Пиши по-русски, структурированно и практично.

            В конце обязательно добавь раздел:
            ## Источники
            И перечисли использованные файлы и разделы.

            Документация проекта:
            $sources

            Вопрос разработчика:
            $question
        """.trimIndent()
    }
}

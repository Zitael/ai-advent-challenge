package ru.maleks.ai_advent_challenge_app.rag.chat

import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContext

class RagChatPromptBuilder {

    fun build(
        question: String,
        state: RagChatState,
        groundedContext: GroundedRagContext
    ): String {
        val history = state.messages
            .takeLast(8)
            .joinToString("\n") { "${it.role}: ${it.content}" }

        val terms = state.taskMemory.fixedTerms.entries.joinToString("\n") {
            "- ${it.key}: ${it.value}"
        }.ifBlank { "- none" }

        val constraints = state.taskMemory.constraints.joinToString("\n") {
            "- $it"
        }.ifBlank { "- none" }

        val clarifications = state.taskMemory.userClarifications.takeLast(10).joinToString("\n") {
            "- $it"
        }.ifBlank { "- none" }

        val sources = groundedContext.sources.joinToString("\n\n") { source ->
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
            Ты RAG-ассистент по локальной базе знаний проекта.

            У тебя есть:
            1. История диалога.
            2. Память задачи.
            3. Найденные источники из RAG.

            Обязательный формат ответа:

            ## Ответ
            Ответ на вопрос пользователя.

            ## Источники
            - source: ...
              section: ...
              chunk_id: ...

            ## Цитаты
            - "точная цитата из источника"

            Правила:
            - Используй только найденные источники и память задачи.
            - Не выдумывай факты.
            - Всегда выводи источники.
            - Всегда выводи цитаты.
            - Если источников недостаточно, скажи: "Не знаю по имеющейся базе знаний. Нужно уточнение."
            - Учитывай цель и ограничения диалога.
            - Не теряй уже зафиксированные термины.

            Память задачи:

            Fixed terms:
            $terms

            Constraints:
            $constraints

            User clarifications:
            $clarifications

            Recent dialog history:
            $history

            RAG sources:
            $sources

            Current user question:
            $question
        """.trimIndent()
    }
}
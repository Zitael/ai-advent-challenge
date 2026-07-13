package ru.maleks.ai_advent_challenge_app.rag.chat

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContextBuilder
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import java.time.Instant

class RagChatService(
    private val ollamaClient: OllamaClient,
    private val index: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val contextBuilder: GroundedRagContextBuilder = GroundedRagContextBuilder(
        minBestScore = 0.08,
        minSources = 1
    ),
    private val promptBuilder: RagChatPromptBuilder = RagChatPromptBuilder(),
    private val memoryUpdater: RagTaskMemoryUpdater = RagTaskMemoryUpdater()
) {

    suspend fun handle(
        userInput: String,
        state: RagChatState
    ): String {
        memoryUpdater.update(
            userInput = userInput,
            memory = state.taskMemory
        )

        val retrieveResult = retriever.retrieve(
            question = userInput,
            index = index,
            searchTopK = 8,
            finalTopK = 3
        )

        val groundedContext = contextBuilder.build(
            question = userInput,
            results = retrieveResult.rerankedResults
        )

        val answer = try {
            if (!groundedContext.enoughContext) {
                buildUnknownAnswer(
                    reason = groundedContext.reason
                )
            } else {
                val prompt = promptBuilder.build(
                    question = userInput,
                    state = state,
                    groundedContext = groundedContext
                )

                ollamaClient.complete(prompt).answer
            }
        } catch (exception: Exception) {
            buildErrorAnswer(exception)
        }

        state.messages.add(
            RagChatMessage(
                role = "user",
                content = userInput,
                createdAt = Instant.now().toString()
            )
        )

        state.messages.add(
            RagChatMessage(
                role = "assistant",
                content = answer,
                createdAt = Instant.now().toString()
            )
        )

        return answer
    }

    private fun buildUnknownAnswer(reason: String): String {
        return """
            ## Ответ

            Не знаю по имеющейся базе знаний. Нужно уточнение.

            ## Причина

            $reason

            ## Источники

            - релевантные источники не найдены

            ## Цитаты

            - цитаты отсутствуют
        """.trimIndent()
    }

    private fun buildErrorAnswer(exception: Exception): String {
        return """
            ## Ответ

            Не удалось получить ответ от локальной LLM.

            ## Ошибка

            ${exception.message ?: exception::class.simpleName ?: "Unknown error"}

            ## Источники

            - ответ не сформирован

            ## Цитаты

            - цитаты отсутствуют
        """.trimIndent()
    }
}
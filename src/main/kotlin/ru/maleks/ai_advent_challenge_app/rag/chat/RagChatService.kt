package ru.maleks.ai_advent_challenge_app.rag.chat

import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContextBuilder
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import java.time.Instant

class RagChatService(
    private val llmClient: OpenRouterClient,
    private val index: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val contextBuilder: GroundedRagContextBuilder = GroundedRagContextBuilder(
        minBestScore = 0.08,
        minSources = 1
    ),
    private val promptBuilder: RagChatPromptBuilder = RagChatPromptBuilder(),
    private val memoryUpdater: RagTaskMemoryUpdater = RagTaskMemoryUpdater()
) {

    suspend fun handle(userInput: String, state: RagChatState): String {
        memoryUpdater.update(userInput, state.taskMemory)

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

        val answer = if (!groundedContext.enoughContext) {
            buildUnknownAnswer(groundedContext.reason)
        } else {
            llmClient.complete(
                listOf(
                    OpenRouterMessage(
                        role = "user",
                        content = promptBuilder.build(
                            question = userInput,
                            state = state,
                            groundedContext = groundedContext
                        )
                    )
                )
            ).answer
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
            - источники не найдены или релевантность ниже порога

            ## Цитаты
            - цитаты отсутствуют
        """.trimIndent()
    }
}
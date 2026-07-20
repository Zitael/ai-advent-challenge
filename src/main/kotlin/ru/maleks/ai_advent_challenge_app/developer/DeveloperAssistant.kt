package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContextBuilder
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever

class DeveloperAssistant(
    private val ollamaClient: OllamaClient,
    private val index: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val gitProjectClient: GitProjectClient,
    private val contextBuilder: GroundedRagContextBuilder = GroundedRagContextBuilder(
        minBestScore = 0.08,
        minSources = 1
    ),
    private val promptBuilder: DeveloperAssistantPromptBuilder = DeveloperAssistantPromptBuilder()
) {

    suspend fun answerProjectQuestion(question: String): String {
        val retrieveResult = retriever.retrieve(
            question = question,
            index = index,
            searchTopK = 10,
            finalTopK = 4
        )

        val groundedContext = contextBuilder.build(
            question = question,
            results = retrieveResult.rerankedResults
        )

        if (!groundedContext.enoughContext) {
            return """
                Не нашёл достаточно информации в README и папке docs.

                Причина: ${groundedContext.reason}

                Попробуй уточнить вопрос или добавить недостающую документацию в project/docs.
            """.trimIndent()
        }

        val branch = gitProjectClient.currentBranch()
        val prompt = promptBuilder.build(
            question = question,
            branch = branch,
            groundedContext = groundedContext
        )

        return ollamaClient.complete(prompt).answer
    }

    suspend fun currentBranch(): String = gitProjectClient.currentBranch()

    suspend fun status(): String = gitProjectClient.status()

    suspend fun diff(): String = gitProjectClient.diff()

    suspend fun files(): String = gitProjectClient.files()
}

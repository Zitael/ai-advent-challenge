package ru.maleks.ai_advent_challenge_app.support

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaOptimizationProfiles
import ru.maleks.ai_advent_challenge_app.promptinjection.InputGuardResult
import ru.maleks.ai_advent_challenge_app.promptinjection.PromptInjectionGuard
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.RerankedSearchResult

class SupportAssistant(
    private val ollamaClient: OllamaClient,
    private val documentationIndex: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val crmClient: SupportCrmClient,
    private val promptBuilder: SupportAssistantPromptBuilder = SupportAssistantPromptBuilder(),
    private val injectionGuard: PromptInjectionGuard = PromptInjectionGuard()
) {
    suspend fun answer(ticketId: String, question: String): String {
        val inputGuard = injectionGuard.inspectInput(question)
        if (inputGuard.blocked) {
            return InputGuardResult.REFUSAL_MESSAGE
        }

        val ticketContext = crmClient.ticketContext(ticketId)
        if (ticketContext.startsWith("Ticket context not found")) {
            return ticketContext
        }

        val ragQuery = buildString {
            appendLine(question)
            appendLine("Ticket context:")
            append(ticketContext)
        }

        val documentationContext = retriever.retrieve(
            question = ragQuery,
            index = documentationIndex,
            searchTopK = 10,
            finalTopK = 5
        ).rerankedResults.joinToString("\n\n") { it.asPromptContext() }
            .ifBlank { "No relevant documentation found." }

        val messages = promptBuilder.buildMessages(
            question = question,
            ticketContext = ticketContext,
            documentationContext = documentationContext,
            mode = PromptSecurityMode.HARDENED
        )

        val answer = ollamaClient.complete(
            messages = messages,
            config = OllamaOptimizationProfiles.optimizedRag
        ).answer

        val outputGuard = injectionGuard.inspectOutput(answer, PromptSecurityMode.HARDENED)
        if (outputGuard.blocked) {
            return InputGuardResult.REFUSAL_MESSAGE
        }

        return answer
    }

    suspend fun listTickets(): String = crmClient.listTickets()

    private fun RerankedSearchResult.asPromptContext(): String = buildString {
        appendLine("Source: ${chunk.source}")
        appendLine("Section: ${chunk.section}")
        appendLine("Relevance: ${"%.4f".format(finalScore)}")
        append(chunk.text.take(MAX_CONTEXT_CHARS))
    }

    private companion object {
        const val MAX_CONTEXT_CHARS = 2_000
    }
}

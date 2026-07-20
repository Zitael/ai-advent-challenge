package ru.maleks.ai_advent_challenge_app.support

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaOptimizationProfiles
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.RerankedSearchResult

class SupportAssistant(
    private val ollamaClient: OllamaClient,
    private val documentationIndex: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val crmClient: SupportCrmClient
) {
    suspend fun answer(ticketId: String, question: String): String {
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

        val prompt = buildPrompt(
            question = question,
            ticketContext = ticketContext,
            documentationContext = documentationContext
        )

        return ollamaClient.complete(
            prompt = prompt,
            config = OllamaOptimizationProfiles.optimizedRag
        ).answer
    }

    suspend fun listTickets(): String = crmClient.listTickets()

    private fun RerankedSearchResult.asPromptContext(): String = buildString {
        appendLine("Source: ${chunk.source}")
        appendLine("Section: ${chunk.section}")
        appendLine("Relevance: ${"%.4f".format(finalScore)}")
        append(chunk.text.take(MAX_CONTEXT_CHARS))
    }

    private fun buildPrompt(
        question: String,
        ticketContext: String,
        documentationContext: String
    ): String = """
        You are a product support assistant.

        Answer in Russian. Use both the CRM ticket context and the product documentation.
        Do not invent account state, incidents or product behavior.
        Clearly distinguish facts from the ticket, facts from documentation and assumptions.
        Give a concise explanation and concrete next steps.
        If the issue requires an operator, say exactly what should be escalated.
        Never expose internal implementation details that are not needed by the user.

        ## User question
        $question

        ## CRM context received through MCP
        $ticketContext

        ## FAQ and documentation retrieved through RAG
        $documentationContext

        Return Markdown with these sections:
        # Что произошло
        # Что сделать
        # Нужна ли эскалация
    """.trimIndent()

    private companion object {
        const val MAX_CONTEXT_CHARS = 2_000
    }
}

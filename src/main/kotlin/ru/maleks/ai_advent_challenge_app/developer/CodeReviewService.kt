package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.RerankedSearchResult

class CodeReviewService(
    private val ollamaClient: OllamaClient,
    private val documentationIndex: DocumentIndex,
    private val codeIndex: DocumentIndex,
    private val retriever: ImprovedRagRetriever
) {

    suspend fun review(
        diff: String,
        changedFiles: List<String>
    ): String {
        if (changedFiles.isEmpty()) {
            return "Нет изменений для ревью."
        }

        if (diff.isBlank()) {
            return """
        Git нашёл изменённые файлы, но не смог сформировать diff.

        Изменённые файлы:
        ${changedFiles.joinToString("\n")}
    """.trimIndent()
        }

        val retrievalQuery = buildString {
            append("Code review for changed files: ")
            append(changedFiles.joinToString(", "))
            append(". Find architectural rules, project conventions, related implementations and possible risks.")
        }

        val documentationContext = retrieveContext(
            query = retrievalQuery,
            index = documentationIndex,
            topK = 5
        )

        val codeContext = retrieveContext(
            query = retrievalQuery + "\n" + diff.take(8_000),
            index = codeIndex,
            topK = 8
        )

        val prompt = buildPrompt(
            diff = diff.take(MAX_DIFF_CHARS),
            changedFiles = changedFiles,
            documentationContext = documentationContext,
            codeContext = codeContext
        )

        return ollamaClient.complete(prompt).answer
    }

    private fun retrieveContext(
        query: String,
        index: DocumentIndex,
        topK: Int
    ): String {
        if (index.chunks.isEmpty()) {
            return "No context found."
        }

        val results = retriever.retrieve(
            question = query,
            index = index,
            searchTopK = topK * 2,
            finalTopK = topK
        ).rerankedResults

        return results.joinToString("\n\n") { it.asPromptContext() }
            .ifBlank { "No relevant context found." }
    }

    private fun RerankedSearchResult.asPromptContext(): String {
        return buildString {
            appendLine("Source: ${chunk.source}")
            appendLine("Section: ${chunk.section}")
            appendLine("Relevance: ${"%.4f".format(finalScore)}")
            append(chunk.text.take(MAX_CONTEXT_CHUNK_CHARS))
        }
    }

    private fun buildPrompt(
        diff: String,
        changedFiles: List<String>,
        documentationContext: String,
        codeContext: String
    ): String {
        return """
            You are a senior Kotlin/Java backend engineer performing a pull request review.

            Review only the supplied changes. Use project documentation and related code as supporting context.
            Do not invent files, requirements or problems that are not supported by the diff or context.
            Prioritize correctness over style. Report only meaningful findings.

            For every finding include:
            - severity: critical, high, medium or low;
            - file name and relevant code fragment when possible;
            - why it is a problem;
            - a concrete recommendation.

            Return Markdown in Russian with exactly these sections:

            # Краткое резюме
            # Потенциальные баги
            # Архитектурные проблемы
            # Рекомендации

            If a section has no findings, write: "Не обнаружено."

            ## Changed files
            ${changedFiles.joinToString("\n") { "- $it" }}

            ## Project documentation retrieved by RAG
            $documentationContext

            ## Related project code retrieved by RAG
            $codeContext

            ## Diff
            ```diff
            $diff
            ```
        """.trimIndent()
    }

    private companion object {
        const val MAX_DIFF_CHARS = 45_000
        const val MAX_CONTEXT_CHUNK_CHARS = 2_500
    }
}

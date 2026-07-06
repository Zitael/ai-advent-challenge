package ru.maleks.ai_advent_challenge_app.rag

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexStorage
import ru.maleks.ai_advent_challenge_app.rag.prompt.RagPromptBuilder
import ru.maleks.ai_advent_challenge_app.rag.qa.ControlQuestionSet
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

suspend fun main(args: Array<String>) {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val model = dotenv["OPENROUTER_MODEL"]
        ?: System.getenv("OPENROUTER_MODEL")
        ?: "openai/gpt-4o-mini"

    val indexPath = Path.of(
        dotenv["RAG_INDEX_PATH"]
            ?: System.getenv("RAG_INDEX_PATH")
            ?: "rag-index/structure-index.json"
    )

    if (!Files.exists(indexPath)) {
        error("RAG index not found: $indexPath. Run ./gradlew runRagIndex first.")
    }

    val question = when {
        args.isNotEmpty() -> args.joinToString(" ")
        Files.exists(Path.of("rag-question.txt")) -> Path.of("rag-question.txt").toFile().readText(Charsets.UTF_8).trim()
        else -> "Чем MCP отличается от REST?"
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    val llmClient = OpenRouterClient(
        httpClient = httpClient,
        apiKey = apiKey,
        model = model
    )

    val embeddingClient = HashEmbeddingClient(dimension = 384)
    val index = DocumentIndexStorage().load(indexPath)
    val searchService = VectorSearchService(embeddingClient)
    val improvedRetriever = ImprovedRagRetriever(searchService)
    val promptBuilder = RagPromptBuilder()

    try {
        println("AI Advent Challenge — Day 23")
        println("Model: $model")
        println("Index: ${indexPath.toAbsolutePath()}")
        println("Question: $question")
        println()

        val noRagPrompt = """
            Ответь на вопрос пользователя без использования внешней базы знаний.

            Вопрос:
            $question
        """.trimIndent()

        val noRagAnswer = llmClient.completeUserPrompt(noRagPrompt)

        val basicResults = searchService.search(
            question = question,
            index = index,
            topK = 3
        )

        val basicRagPrompt = promptBuilder.build(
            question = question,
            results = basicResults
        )

        val basicRagAnswer = llmClient.completeUserPrompt(basicRagPrompt)

        val improvedResult = improvedRetriever.retrieve(
            question = question,
            index = index,
            searchTopK = 8,
            finalTopK = 3
        )

        val improvedRagPrompt = promptBuilder.buildFromReranked(
            question = question,
            results = improvedResult.rerankedResults
        )

        val improvedRagAnswer = llmClient.completeUserPrompt(improvedRagPrompt)

        printConsoleReport(
            question = question,
            noRagAnswer = noRagAnswer,
            basicResults = basicResults,
            basicRagAnswer = basicRagAnswer,
            improvedResult = improvedResult,
            improvedRagAnswer = improvedRagAnswer
        )

        saveReport(
            question = question,
            noRagAnswer = noRagAnswer,
            basicResults = basicResults,
            basicRagAnswer = basicRagAnswer,
            improvedResult = improvedResult,
            improvedRagAnswer = improvedRagAnswer
        )
    } finally {
        httpClient.close()
    }
}

private fun printConsoleReport(
    question: String,
    noRagAnswer: String,
    basicResults: List<ru.maleks.ai_advent_challenge_app.rag.search.SearchResult>,
    basicRagAnswer: String,
    improvedResult: ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRetrieveResult,
    improvedRagAnswer: String
) {
    println("========== WITHOUT RAG ==========")
    println(noRagAnswer)
    println()

    println("========== BASIC RAG CHUNKS ==========")
    basicResults.forEachIndexed { i, result ->
        println(
            "${i + 1}. ${result.chunk.source} | section=${result.chunk.section} | similarity=${
                "%.4f".format(result.score)
            }"
        )
    }
    println()

    println("========== BASIC RAG ANSWER ==========")
    println(basicRagAnswer)
    println()

    println("========== IMPROVED RAG ==========")
    println("Original question: $question")
    println("Rewritten query: ${improvedResult.rewrittenQuery}")
    println("Raw topK before filter: ${improvedResult.rawResults.size}")
    println("After similarity filter: ${improvedResult.filteredResults.size}")
    println("Final topK after rerank: ${improvedResult.rerankedResults.size}")
    println()

    println("========== IMPROVED RAG CHUNKS ==========")
    improvedResult.rerankedResults.forEachIndexed { i, result ->
        println(
            "${i + 1}. ${result.chunk.source} | section=${result.chunk.section} | similarity=${
                "%.4f".format(result.similarityScore)
            } | keyword=${"%.4f".format(result.keywordScore)} | final=${"%.4f".format(result.finalScore)}"
        )
    }
    println()

    println("========== IMPROVED RAG ANSWER ==========")
    println(improvedRagAnswer)
    println()

    println("========== CONTROL QUESTIONS ==========")
    ControlQuestionSet.questions().forEach { control ->
        println("${control.id}. ${control.question}")
        println("   expected contains: ${control.expectedAnswerShouldContain.joinToString(", ")}")
        println("   expected sources: ${control.expectedSources.joinToString(", ")}")
    }
}

private fun saveReport(
    question: String,
    noRagAnswer: String,
    basicResults: List<ru.maleks.ai_advent_challenge_app.rag.search.SearchResult>,
    basicRagAnswer: String,
    improvedResult: ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRetrieveResult,
    improvedRagAnswer: String
) {
    val outputDirectory = Path.of("rag-index")
    Files.createDirectories(outputDirectory)

    val report = """
        # Day 23 — RAG reranking and filtering report

        ## Question

        $question

        ## Without RAG

        $noRagAnswer

        ## Basic RAG retrieved chunks

        ${basicResults.joinToString("\n") { result ->
        "- ${result.chunk.source} | section=${result.chunk.section} | similarity=${"%.4f".format(result.score)}"
    }}

        ## Basic RAG answer

        $basicRagAnswer

        ## Improved RAG settings

        - query rewrite: enabled
        - raw topK before filter: ${improvedResult.rawResults.size}
        - similarity threshold: 0.08
        - chunks after filtering: ${improvedResult.filteredResults.size}
        - final topK after reranking: ${improvedResult.rerankedResults.size}

        ## Rewritten query

        ${improvedResult.rewrittenQuery}

        ## Improved RAG retrieved chunks

        ${improvedResult.rerankedResults.joinToString("\n") { result ->
        "- ${result.chunk.source} | section=${result.chunk.section} | similarity=${"%.4f".format(result.similarityScore)} | keyword=${"%.4f".format(result.keywordScore)} | final=${"%.4f".format(result.finalScore)}"
    }}

        ## Improved RAG answer

        $improvedRagAnswer
    """.trimIndent()

    val reportPath = outputDirectory.resolve("day23-rag-rerank-report.md")
    reportPath.writeText(report, Charsets.UTF_8)

    println()
    println("Report saved to: ${reportPath.toAbsolutePath()}")
}

private suspend fun OpenRouterClient.completeUserPrompt(prompt: String): String {
    return complete(
        messages = listOf(
            OpenRouterMessage(
                role = "user",
                content = prompt
            )
        )
    ).answer
}
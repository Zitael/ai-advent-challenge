package ru.maleks.ai_advent_challenge_app.rag

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContextBuilder
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
    val dotenv = dotenv { ignoreIfMissing = true }

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
    val groundedContextBuilder = GroundedRagContextBuilder(
        minBestScore = 0.08,
        minSources = 1
    )
    val promptBuilder = RagPromptBuilder()

    try {
        val improvedResult = improvedRetriever.retrieve(
            question = question,
            index = index,
            searchTopK = 8,
            finalTopK = 3
        )

        val groundedContext = groundedContextBuilder.build(
            question = question,
            results = improvedResult.rerankedResults
        )

        val groundedAnswer = if (!groundedContext.enoughContext) {
            """
            ## Ответ
            Не знаю по имеющейся базе знаний. Нужно уточнение.

            ## Причина
            ${groundedContext.reason}

            ## Источники
            ${groundedContext.sources.joinToString("\n") { "- source: ${it.source}, section: ${it.section}, chunk_id: ${it.chunkId}" }.ifBlank { "- источники не найдены" }}

            ## Цитаты
            ${groundedContext.sources.joinToString("\n") { "- \"${it.quote}\"" }.ifBlank { "- цитаты отсутствуют" }}
            """.trimIndent()
        } else {
            llmClient.completeUserPrompt(
                promptBuilder.buildGroundedPrompt(groundedContext)
            )
        }

        val validation = validateGroundedAnswer(groundedAnswer)

        saveDay24Report(
            question = question,
            rewrittenQuery = improvedResult.rewrittenQuery,
            groundedContext = groundedContext,
            groundedAnswer = groundedAnswer,
            validation = validation
        )

        println("AI Advent Challenge - Day 24")
        println("Question loaded. See report: rag-index/day24-grounded-rag-report.md")
        println("Enough context: ${groundedContext.enoughContext}")
        println("Reason: ${groundedContext.reason}")
        println("Sources: ${groundedContext.sources.size}")
        println("Has sources block: ${validation.hasSourcesBlock}")
        println("Has quotes block: ${validation.hasQuotesBlock}")
    } finally {
        httpClient.close()
    }
}

private data class GroundedAnswerValidation(
    val hasSourcesBlock: Boolean,
    val hasQuotesBlock: Boolean,
    val hasUnknownMode: Boolean
)

private fun validateGroundedAnswer(answer: String): GroundedAnswerValidation {
    return GroundedAnswerValidation(
        hasSourcesBlock = answer.contains("## Источники", ignoreCase = true),
        hasQuotesBlock = answer.contains("## Цитаты", ignoreCase = true),
        hasUnknownMode = answer.contains("Не знаю", ignoreCase = true)
    )
}

private fun saveDay24Report(
    question: String,
    rewrittenQuery: String,
    groundedContext: ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContext,
    groundedAnswer: String,
    validation: GroundedAnswerValidation
) {
    val outputDirectory = Path.of("rag-index")
    Files.createDirectories(outputDirectory)

    val report = """
        # Day 24 — Grounded RAG: citations, sources and anti-hallucination

        ## Question

        $question

        ## Rewritten query

        $rewrittenQuery

        ## Context decision

        - enough context: ${groundedContext.enoughContext}
        - reason: ${groundedContext.reason}

        ## Retrieved sources

        ${groundedContext.sources.joinToString("\n\n") { source ->
        """
            ### Source

            - source: ${source.source}
            - title: ${source.title}
            - section: ${source.section}
            - chunk_id: ${source.chunkId}
            - score: ${"%.4f".format(source.score)}

            Quote:

            > ${source.quote}
            """.trimIndent()
    }}

        ## Answer

        $groundedAnswer

        ## Validation

        - has sources block: ${validation.hasSourcesBlock}
        - has quotes block: ${validation.hasQuotesBlock}
        - unknown mode: ${validation.hasUnknownMode}

        ## Control questions

        ${ControlQuestionSet.questions().joinToString("\n") { question ->
        "- ${question.id}. ${question.question}; expected sources: ${question.expectedSources.joinToString(", ")}"
    }}
    """.trimIndent()

    outputDirectory.resolve("day24-grounded-rag-report.md").writeText(report, Charsets.UTF_8)
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
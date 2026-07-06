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
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Files
import java.nio.file.Path

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

    val question = args.joinToString(" ").ifBlank {
        "Чем MCP отличается от REST?"
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
    val promptBuilder = RagPromptBuilder()

    try {
        println("AI Advent Challenge — Day 22")
        println("Model: $model")
        println("Index: ${indexPath.toAbsolutePath()}")
        println("Question: $question")
        println()

        val noRagPrompt = """
            Ответь на вопрос пользователя без использования внешней базы знаний.

            Вопрос:
            $question
        """.trimIndent()

        println("========== WITHOUT RAG ==========")
        val noRagAnswer = llmClient.completeUserPrompt(noRagPrompt)
        println(noRagAnswer)
        println()

        val results = searchService.search(
            question = question,
            index = index,
            topK = 3
        )

        println("========== RETRIEVED CHUNKS ==========")
        results.forEachIndexed { i, result ->
            println(
                "${i + 1}. ${result.chunk.source} | section=${result.chunk.section} | score=${
                    "%.4f".format(result.score)
                }"
            )
        }
        println()

        val ragPrompt = promptBuilder.build(
            question = question,
            results = results
        )

        println("========== WITH RAG ==========")
        val ragAnswer = llmClient.completeUserPrompt(ragPrompt)
        println(ragAnswer)
        println()

        println("========== CONTROL QUESTIONS ==========")
        ControlQuestionSet.questions().forEach { control ->
            println("${control.id}. ${control.question}")
            println("   expected contains: ${control.expectedAnswerShouldContain.joinToString(", ")}")
            println("   expected sources: ${control.expectedSources.joinToString(", ")}")
        }
    } finally {
        httpClient.close()
    }
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
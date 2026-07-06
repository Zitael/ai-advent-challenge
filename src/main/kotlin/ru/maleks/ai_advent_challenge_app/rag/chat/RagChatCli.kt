package ru.maleks.ai_advent_challenge_app.rag.chat

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.client.plugins.HttpTimeout
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexStorage
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

suspend fun main() {
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

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
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
    val retriever = ImprovedRagRetriever(searchService)
    val storage = RagChatStorage()
    val service = RagChatService(
        llmClient = llmClient,
        index = index,
        retriever = retriever
    )

    var state = storage.load()

    println("AI Advent Challenge - Day 25")
    println("Mini RAG chat started.")

    println("Commands:")
    println("  /exit")
    println("  /clear")
    println("  /memory")
    println("  /report")
    println()
    println("Tip:")
    println("  goal: explain RAG architecture")
    println("  term RAG=Retrieval Augmented Generation")
    println("  constraint: always compare with MCP")
    println()

    try {
        while (true) {
            print("You: ")
            val input = readlnOrNull()?.trim()

            if (input == null) {
                println("Input stream closed. Use /report file mode or run from real terminal.")
                break
            }

            if (input.isBlank()) {
                continue
            }

            when {
                input.equals("/exit", ignoreCase = true) -> break

                input.equals("/clear", ignoreCase = true) -> {
                    storage.clear()
                    state = RagChatState()
                    println("Chat state cleared.")
                    continue
                }

                input.equals("/memory", ignoreCase = true) -> {
                    printMemory(state)
                    continue
                }

                input.equals("/report", ignoreCase = true) -> {
                    saveReport(state)
                    println("Report saved to rag-index/day25-rag-chat-report.md")
                    continue
                }
            }


            val answer = try {
                service.handle(input, state)
            } catch (e: Exception) {
                """
                    ## Ответ
                    Ошибка при обращении к LLM: ${e.message}

                    ## Источники
                    - не использовались

                    ## Цитаты
                    - отсутствуют
                """.trimIndent()
            }
            storage.save(state)

            println()
            println("========================================")
            println("Assistant")
            println("========================================")
            println(answer)
            println()
        }
    } finally {
        saveReport(state)
        httpClient.close()
    }
}

private fun printMemory(state: RagChatState) {
    println()
    println("========== TASK MEMORY ==========")
    println("Fixed terms:")
    state.taskMemory.fixedTerms.forEach { (key, value) ->
        println("- $key = $value")
    }

    println("Constraints:")
    state.taskMemory.constraints.forEach {
        println("- $it")
    }

    println("Clarifications:")
    state.taskMemory.userClarifications.forEach {
        println("- $it")
    }

    println("Messages: ${state.messages.size}")
    println("=================================")
    println()
}

private fun saveReport(state: RagChatState) {
    val outputDirectory = Path.of("rag-index")
    Files.createDirectories(outputDirectory)

    val report = """
        # Day 25 — Mini RAG chat with task memory

        ## Task memory

        ### Fixed terms

        ${state.taskMemory.fixedTerms.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }.ifBlank { "- none" }}

        ### Constraints

        ${state.taskMemory.constraints.joinToString("\n") { "- $it" }.ifBlank { "- none" }}

        ### User clarifications

        ${state.taskMemory.userClarifications.joinToString("\n") { "- $it" }.ifBlank { "- none" }}

        ## Dialog history

        ${state.messages.joinToString("\n\n") { message ->
        """
            ### ${message.role} — ${message.createdAt}

            ${message.content}
            """.trimIndent()
    }}
    """.trimIndent()

    outputDirectory.resolve("day25-rag-chat-report.md").writeText(report, Charsets.UTF_8)
}
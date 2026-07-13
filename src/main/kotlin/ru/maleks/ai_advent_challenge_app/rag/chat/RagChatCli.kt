package ru.maleks.ai_advent_challenge_app.rag.chat

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexStorage
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

suspend fun main() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val ollamaBaseUrl = dotenv["OLLAMA_BASE_URL"]
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434"

    val ollamaModel = dotenv["OLLAMA_MODEL"]
        ?: System.getenv("OLLAMA_MODEL")
        ?: "qwen3:8b"

    val indexPath = Path.of(
        dotenv["RAG_INDEX_PATH"]
            ?: System.getenv("RAG_INDEX_PATH")
            ?: "rag-index/structure-index.json"
    )

    if (!Files.exists(indexPath)) {
        error(
            "RAG index not found: $indexPath. " +
                    "Run ./gradlew runRagIndex first."
        )
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }

        expectSuccess = true
    }

    val ollamaClient = OllamaClient(
        httpClient = httpClient,
        baseUrl = ollamaBaseUrl.removeSuffix("/"),
        model = ollamaModel
    )

    val embeddingClient = HashEmbeddingClient(
        dimension = 384
    )

    val index = DocumentIndexStorage().load(indexPath)

    val searchService = VectorSearchService(
        embeddingClient = embeddingClient
    )

    val retriever = ImprovedRagRetriever(
        vectorSearchService = searchService
    )

    val storage = RagChatStorage()

    val service = RagChatService(
        ollamaClient = ollamaClient,
        index = index,
        retriever = retriever
    )

    var state = storage.load()

    val reader = System.`in`.bufferedReader()

    println("AI Advent Challenge - Day 27")
    println("Local RAG Chat")
    println("Ollama URL: $ollamaBaseUrl")
    println("Local model: $ollamaModel")
    println("Cloud LLM: disabled")
    println("RAG index: ${indexPath.toAbsolutePath()}")
    println()
    println("Commands:")
    println("  /clear  - clear chat history and task memory")
    println("  /memory - show task memory")
    println("  /report - save readable Markdown report")
    println("  /exit   - exit chat")
    println()
    println("Task memory commands:")
    println("  goal: <dialog goal>")
    println("  term <name>=<definition>")
    println("  constraint: <constraint>")
    println()

    try {
        while (true) {
            print("You: ")
            System.out.flush()

            val rawInput = reader.readLine()

            if (rawInput == null) {
                println()
                println("Input stream closed.")
                break
            }

            val input = rawInput.trim()

            if (input.isBlank()) {
                continue
            }

            when {
                input.equals("/exit", ignoreCase = true) -> {
                    break
                }

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
                    saveReport(
                        state = state,
                        ollamaModel = ollamaModel,
                        ollamaBaseUrl = ollamaBaseUrl
                    )

                    println(
                        "Report saved to " +
                                "rag-index/day27-local-rag-chat-report.md"
                    )
                    continue
                }
            }

            println()
            println("Local model is generating an answer...")

            val answer = service.handle(
                userInput = input,
                state = state
            )

            storage.save(state)

            println()
            println("Assistant:")
            println(answer)
            println()
        }
    } finally {
        saveReport(
            state = state,
            ollamaModel = ollamaModel,
            ollamaBaseUrl = ollamaBaseUrl
        )

        httpClient.close()

        println()
        println(
            "Final report saved to " +
                    "rag-index/day27-local-rag-chat-report.md"
        )
    }
}

private fun printMemory(state: RagChatState) {
    println()
    println("========== TASK MEMORY ==========")

    println("Goal:")
    println(
        state.taskMemory.goal.ifBlank {
            "not specified"
        }
    )

    println()
    println("Fixed terms:")

    if (state.taskMemory.fixedTerms.isEmpty()) {
        println("- none")
    } else {
        state.taskMemory.fixedTerms.forEach { (key, value) ->
            println("- $key = $value")
        }
    }

    println()
    println("Constraints:")

    if (state.taskMemory.constraints.isEmpty()) {
        println("- none")
    } else {
        state.taskMemory.constraints.forEach { constraint ->
            println("- $constraint")
        }
    }

    println()
    println("Clarifications:")

    if (state.taskMemory.userClarifications.isEmpty()) {
        println("- none")
    } else {
        state.taskMemory.userClarifications.forEach { clarification ->
            println("- $clarification")
        }
    }

    println()
    println("Stored messages: ${state.messages.size}")
    println("=================================")
    println()
}

private fun saveReport(
    state: RagChatState,
    ollamaModel: String,
    ollamaBaseUrl: String
) {
    val outputDirectory = Path.of("rag-index")
    Files.createDirectories(outputDirectory)

    val fixedTerms = state.taskMemory.fixedTerms.entries
        .joinToString("\n") { entry ->
            "- ${entry.key}: ${entry.value}"
        }
        .ifBlank {
            "- none"
        }

    val constraints = state.taskMemory.constraints
        .joinToString("\n") { constraint ->
            "- $constraint"
        }
        .ifBlank {
            "- none"
        }

    val clarifications = state.taskMemory.userClarifications
        .joinToString("\n") { clarification ->
            "- $clarification"
        }
        .ifBlank {
            "- none"
        }

    val dialog = state.messages
        .joinToString("\n\n") { message ->
            """
                ### ${message.role} — ${message.createdAt}

                ${message.content}
            """.trimIndent()
        }
        .ifBlank {
            "Dialog is empty."
        }

    val report = """
        # Day 27 — Local LLM application

        ## Configuration

        - application: local RAG CLI chat
        - LLM provider: Ollama
        - model: `$ollamaModel`
        - API: `$ollamaBaseUrl`
        - cloud models: disabled
        - stored messages: ${state.messages.size}

        ## Task memory

        ### Goal

        ${state.taskMemory.goal.ifBlank { "not specified" }}

        ### Fixed terms

        $fixedTerms

        ### Constraints

        $constraints

        ### User clarifications

        $clarifications

        ## Dialog

        $dialog
    """.trimIndent()

    outputDirectory
        .resolve("day27-local-rag-chat-report.md")
        .writeText(
            report,
            Charsets.UTF_8
        )
}
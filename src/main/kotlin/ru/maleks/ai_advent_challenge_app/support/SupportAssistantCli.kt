package ru.maleks.ai_advent_challenge_app.support

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.mcp.client.McpServerConfig
import ru.maleks.ai_advent_challenge_app.mcp.client.RemoteMcpClient
import ru.maleks.ai_advent_challenge_app.mcp.support.SupportMcpServerFactory
import ru.maleks.ai_advent_challenge_app.mcp.support.SupportMcpServerRunner
import ru.maleks.ai_advent_challenge_app.rag.chunk.MarkdownStructureChunker
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexer
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val projectRoot = Path.of(dotenv["PROJECT_ROOT"] ?: System.getenv("PROJECT_ROOT") ?: ".")
        .toAbsolutePath().normalize()
    val dataFile = projectRoot.resolve("support-data/crm.json")
    val ollamaBaseUrl = dotenv["OLLAMA_BASE_URL"] ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434"
    val ollamaModel = dotenv["OLLAMA_MODEL"] ?: System.getenv("OLLAMA_MODEL") ?: "qwen3:8b"
    val mcpPort = (dotenv["SUPPORT_MCP_PORT"] ?: System.getenv("SUPPORT_MCP_PORT") ?: "3020").toInt()

    val documents = SupportDocumentLoader(projectRoot).load()
    require(documents.isNotEmpty()) { "No support documentation found in support-docs/." }

    val embeddingClient = HashEmbeddingClient(dimension = 384)
    val index = DocumentIndexer(embeddingClient).buildIndex(
        documents = documents,
        strategy = MarkdownStructureChunker(maxSectionWords = 300, overlapWords = 40),
        sourceDirectory = projectRoot.resolve("support-docs").toString()
    )

    val crmRepository = SupportCrmRepository(dataFile)
    val mcpServer = SupportMcpServerRunner(
        host = "127.0.0.1",
        port = mcpPort,
        path = "/mcp",
        factory = SupportMcpServerFactory(crmRepository)
    )
    mcpServer.start()

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }
        expectSuccess = true
    }

    val assistant = SupportAssistant(
        ollamaClient = OllamaClient(httpClient, ollamaBaseUrl.removeSuffix("/"), ollamaModel),
        documentationIndex = index,
        retriever = ImprovedRagRetriever(VectorSearchService(embeddingClient)),
        crmClient = SupportCrmClient(
            RemoteMcpClient(
                McpServerConfig(
                    url = mcpServer.url,
                    clientName = "support-assistant",
                    clientVersion = "1.0.0"
                )
            )
        )
    )

    val reader = System.`in`.bufferedReader()
    println()
    println("AI Advent Challenge — Day 33")
    println("User Support Assistant")
    println("Model: $ollamaModel")
    println("Indexed support documents: ${documents.size}")
    println("MCP: ${mcpServer.url}")
    println("Commands: /tickets, /ticket <id>, /exit")

    var activeTicketId: String? = null

    try {
        while (true) {
            print("\nYou: ")
            System.out.flush()
            val input = reader.readLine()?.trim() ?: break
            if (input.isBlank()) continue

            when {
                input.equals("/exit", ignoreCase = true) -> break
                input.equals("/tickets", ignoreCase = true) -> println(assistant.listTickets())
                input.startsWith("/ticket ", ignoreCase = true) -> {
                    activeTicketId = input.substringAfter(' ').trim()
                    println("Active ticket: $activeTicketId")
                }
                activeTicketId == null -> println("Select a ticket first: /ticket T-1001")
                else -> {
                    println("\nSearching CRM through MCP and documentation through RAG...")
                    println("\nAssistant:\n${assistant.answer(activeTicketId, input)}")
                }
            }
        }
    } finally {
        httpClient.close()
        mcpServer.stop()
    }
}

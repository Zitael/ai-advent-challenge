package ru.maleks.ai_advent_challenge_app.developer

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.mcp.client.McpServerConfig
import ru.maleks.ai_advent_challenge_app.mcp.client.RemoteMcpClient
import ru.maleks.ai_advent_challenge_app.mcp.project.ProjectMcpServerFactory
import ru.maleks.ai_advent_challenge_app.mcp.project.ProjectMcpServerRunner
import ru.maleks.ai_advent_challenge_app.rag.chunk.FixedSizeChunker
import ru.maleks.ai_advent_challenge_app.rag.chunk.MarkdownStructureChunker
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexer
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }

    val projectRoot = Path.of(
        dotenv["PROJECT_ROOT"]
            ?: System.getenv("PROJECT_ROOT")
            ?: "."
    ).toAbsolutePath().normalize()

    val ollamaBaseUrl = dotenv["OLLAMA_BASE_URL"]
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434"

    val ollamaModel = dotenv["OLLAMA_MODEL"]
        ?: System.getenv("OLLAMA_MODEL")
        ?: "qwen3:8b"

    val mcpPort = (dotenv["PROJECT_MCP_PORT"]
        ?: System.getenv("PROJECT_MCP_PORT")
        ?: "3010").toInt()

    val documentation = ProjectDocumentLoader(projectRoot).load()
    require(documentation.isNotEmpty()) {
        "No project documentation found. Add README.md and files to docs/."
    }

    val codeDocuments = ProjectCodeLoader(projectRoot).load()

    println("Indexing project documentation and code...")
    val embeddingClient = HashEmbeddingClient(dimension = 384)
    val documentIndexer = DocumentIndexer(embeddingClient)

    val documentationIndex = documentIndexer.buildIndex(
        documents = documentation,
        strategy = MarkdownStructureChunker(
            maxSectionWords = 350,
            overlapWords = 40
        ),
        sourceDirectory = projectRoot.toString()
    )

    val codeIndex = documentIndexer.buildIndex(
        documents = codeDocuments,
        strategy = FixedSizeChunker(
            chunkSizeWords = 260,
            overlapWords = 50
        ),
        sourceDirectory = projectRoot.toString()
    )

    val mcpServer = ProjectMcpServerRunner(
        host = "127.0.0.1",
        port = mcpPort,
        path = "/mcp",
        factory = ProjectMcpServerFactory(projectRoot)
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

    val ollamaClient = OllamaClient(
        httpClient = httpClient,
        baseUrl = ollamaBaseUrl.removeSuffix("/"),
        model = ollamaModel
    )

    val retriever = ImprovedRagRetriever(
        vectorSearchService = VectorSearchService(embeddingClient)
    )

    val mcpClient = RemoteMcpClient(
        McpServerConfig(
            url = mcpServer.url,
            clientName = "developer-assistant",
            clientVersion = "1.0.0"
        )
    )

    val codeReviewService = CodeReviewService(
        ollamaClient = ollamaClient,
        documentationIndex = documentationIndex,
        codeIndex = codeIndex,
        retriever = retriever
    )

    val assistant = DeveloperAssistant(
        ollamaClient = ollamaClient,
        index = documentationIndex,
        retriever = retriever,
        gitProjectClient = GitProjectClient(mcpClient),
        codeReviewService = codeReviewService,
        gitDiffProvider = GitDiffProvider(projectRoot)
    )

    val parser = DeveloperCommandParser()
    val reader = System.`in`.bufferedReader()

    println()
    println("AI Advent Challenge — Day 32")
    println("Developer Assistant with AI Code Review")
    println("Project: $projectRoot")
    println("Indexed docs: ${documentation.size}")
    println("Indexed code files: ${codeDocuments.size}")
    println("Model: $ollamaModel")
    println("MCP: ${mcpServer.url}")
    printCommands()

    try {
        while (true) {
            print("\nYou: ")
            System.out.flush()

            val input = reader.readLine()?.trim() ?: break
            if (input.isBlank()) continue

            when (val command = parser.parse(input)) {
                DeveloperCommand.Exit -> break
                DeveloperCommand.Commands -> printCommands()
                DeveloperCommand.Branch -> println(assistant.currentBranch())
                DeveloperCommand.Status -> println(assistant.status())
                DeveloperCommand.Diff -> println(assistant.diff())
                DeveloperCommand.Files -> println(assistant.files())
                DeveloperCommand.Review -> {
                    println("\nReviewing staged and unstaged changes...")
                    println("\nAssistant:\n${assistant.reviewLocalChanges()}")
                }
                is DeveloperCommand.Help -> {
                    println("\nSearching README and docs...")
                    println("\nAssistant:\n${assistant.answerProjectQuestion(command.question)}")
                }
                is DeveloperCommand.Unknown -> {
                    println("Unknown command: ${command.raw}")
                    println("Use /commands or ask: /help <question>")
                }
            }
        }
    } finally {
        httpClient.close()
        mcpServer.stop()
    }
}

private fun printCommands() {
    println()
    println("Commands:")
    println("  /help <question> — answer using README and docs")
    println("  /review          — review staged and unstaged local changes")
    println("  /branch          — current git branch through MCP")
    println("  /status          — git status through MCP")
    println("  /diff            — current git diff through MCP")
    println("  /files           — project files through MCP")
    println("  /commands        — show commands")
    println("  /exit            — exit")
}

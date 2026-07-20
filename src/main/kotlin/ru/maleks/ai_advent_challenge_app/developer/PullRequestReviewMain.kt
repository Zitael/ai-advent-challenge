package ru.maleks.ai_advent_challenge_app.developer

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.rag.chunk.FixedSizeChunker
import ru.maleks.ai_advent_challenge_app.rag.chunk.MarkdownStructureChunker
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexer
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.rag.search.VectorSearchService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val projectRoot = Path.of(
        System.getenv("PROJECT_ROOT")
            ?: dotenv["PROJECT_ROOT"]
            ?: "."
    ).toAbsolutePath().normalize()

    val baseRef = System.getenv("REVIEW_BASE_REF")
        ?: error("REVIEW_BASE_REF is required, for example: main")

    val outputPath = Path.of(
        System.getenv("REVIEW_OUTPUT") ?: "build/reports/ai-code-review.md"
    )

    val ollamaBaseUrl = System.getenv("OLLAMA_BASE_URL")
        ?: dotenv["OLLAMA_BASE_URL"]
        ?: "http://localhost:11434"

    val ollamaModel = System.getenv("OLLAMA_MODEL")
        ?: dotenv["OLLAMA_MODEL"]
        ?: "qwen3:8b"

    val documentation = ProjectDocumentLoader(projectRoot).load()
    val codeDocuments = ProjectCodeLoader(projectRoot).load()
    val embeddingClient = HashEmbeddingClient(dimension = 384)
    val indexer = DocumentIndexer(embeddingClient)

    val documentationIndex = indexer.buildIndex(
        documents = documentation,
        strategy = MarkdownStructureChunker(maxSectionWords = 350, overlapWords = 40),
        sourceDirectory = projectRoot.toString()
    )

    val codeIndex = indexer.buildIndex(
        documents = codeDocuments,
        strategy = FixedSizeChunker(chunkSizeWords = 260, overlapWords = 50),
        sourceDirectory = projectRoot.toString()
    )

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }
        expectSuccess = true
    }

    try {
        val ollamaClient = OllamaClient(
            httpClient = httpClient,
            baseUrl = ollamaBaseUrl.removeSuffix("/"),
            model = ollamaModel
        )

        val reviewService = CodeReviewService(
            ollamaClient = ollamaClient,
            documentationIndex = documentationIndex,
            codeIndex = codeIndex,
            retriever = ImprovedRagRetriever(VectorSearchService(embeddingClient))
        )

        val changes = GitDiffProvider(projectRoot).pullRequestChanges(baseRef)
        val review = reviewService.review(changes.diff, changes.changedFiles)

        Files.createDirectories(outputPath.toAbsolutePath().parent)
        outputPath.writeText(review)
        println(review)
        println("\nReview saved to: ${outputPath.toAbsolutePath()}")
    } finally {
        httpClient.close()
    }
}

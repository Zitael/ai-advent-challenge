package ru.maleks.ai_advent_challenge_app.rag

import ru.maleks.ai_advent_challenge_app.rag.chunk.FixedSizeChunker
import ru.maleks.ai_advent_challenge_app.rag.chunk.MarkdownStructureChunker
import ru.maleks.ai_advent_challenge_app.rag.document.DocumentLoader
import ru.maleks.ai_advent_challenge_app.rag.embedding.HashEmbeddingClient
import ru.maleks.ai_advent_challenge_app.rag.index.ChunkingComparisonReport
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexer
import ru.maleks.ai_advent_challenge_app.rag.index.DocumentIndexStorage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

fun main() {
    val knowledgeDirectory = Path.of("knowledge")
    val outputDirectory = Path.of("rag-index")

    println("AI Advent Challenge — Day 21")
    println("Knowledge directory: ${knowledgeDirectory.toAbsolutePath()}")
    println("Output directory: ${outputDirectory.toAbsolutePath()}")
    println()

    val documents = DocumentLoader(knowledgeDirectory).load()

    if (documents.isEmpty()) {
        error("No documents found in $knowledgeDirectory")
    }

    println("Loaded documents: ${documents.size}")
    documents.forEach { document ->
        println("- ${document.source} | title='${document.title}' | words=${document.text.wordCount()}")
    }
    println()

    val embeddingClient = HashEmbeddingClient(dimension = 384)
    val indexer = DocumentIndexer(embeddingClient)
    val storage = DocumentIndexStorage()

    val fixedStrategy = FixedSizeChunker(
        chunkSizeWords = 220,
        overlapWords = 40
    )

    val structureStrategy = MarkdownStructureChunker(
        maxSectionWords = 350,
        overlapWords = 40
    )

    val fixedIndex = indexer.buildIndex(
        documents = documents,
        strategy = fixedStrategy,
        sourceDirectory = knowledgeDirectory.toString()
    )

    val structureIndex = indexer.buildIndex(
        documents = documents,
        strategy = structureStrategy,
        sourceDirectory = knowledgeDirectory.toString()
    )

    Files.createDirectories(outputDirectory)

    val fixedPath = outputDirectory.resolve("fixed-index.json")
    val structurePath = outputDirectory.resolve("structure-index.json")
    val comparisonPath = outputDirectory.resolve("chunking-comparison.txt")

    storage.save(fixedIndex, fixedPath)
    storage.save(structureIndex, structurePath)

    comparisonPath.writeText(
        ChunkingComparisonReport.build(fixedIndex, structureIndex)
    )

    println()
    println("========== RAG INDEX RESULT ==========")
    println("Fixed-size index: ${fixedPath.toAbsolutePath()}")
    println("Structure index: ${structurePath.toAbsolutePath()}")
    println("Comparison report: ${comparisonPath.toAbsolutePath()}")
    println()
    println("Fixed-size chunks: ${fixedIndex.stats.chunks}")
    println("Structure chunks: ${structureIndex.stats.chunks}")
    println("Embedding model: ${embeddingClient.modelName}")
    println("======================================")
}

private fun String.wordCount(): Int {
    return split(Regex("\\s+"))
        .count { it.isNotBlank() }
}
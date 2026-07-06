package ru.maleks.ai_advent_challenge_app.rag.chunk

import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentChunk

class MarkdownStructureChunker(
    private val maxSectionWords: Int = 350,
    private val overlapWords: Int = 40
) : ChunkingStrategy {

    override val name: String = "markdown-structure"

    override fun chunk(documents: List<RawDocument>): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()

        documents.forEach { document ->
            val sections = splitByMarkdownHeadings(document)
            var localChunkNumber = 1
            var globalWordOffset = 0

            sections.forEach { section ->
                val words = section.text.toWords()

                if (words.isEmpty()) {
                    return@forEach
                }

                if (words.size <= maxSectionWords) {
                    chunks.add(
                        DocumentChunk(
                            chunkId = "${document.source}::$name::$localChunkNumber",
                            source = document.source,
                            title = document.title,
                            section = section.title,
                            strategy = name,
                            text = section.text.trim(),
                            startWord = globalWordOffset,
                            endWord = globalWordOffset + words.size
                        )
                    )
                    localChunkNumber++
                    globalWordOffset += words.size
                } else {
                    var start = 0

                    while (start < words.size) {
                        val end = minOf(start + maxSectionWords, words.size)

                        chunks.add(
                            DocumentChunk(
                                chunkId = "${document.source}::$name::$localChunkNumber",
                                source = document.source,
                                title = document.title,
                                section = section.title,
                                strategy = name,
                                text = words.subList(start, end).joinToString(" "),
                                startWord = globalWordOffset + start,
                                endWord = globalWordOffset + end
                            )
                        )

                        localChunkNumber++

                        if (end == words.size) {
                            break
                        }

                        start = (end - overlapWords).coerceAtLeast(start + 1)
                    }

                    globalWordOffset += words.size
                }
            }
        }

        return chunks
    }

    private fun splitByMarkdownHeadings(document: RawDocument): List<Section> {
        val result = mutableListOf<Section>()

        var currentTitle = document.title
        val currentLines = mutableListOf<String>()

        document.text.lines().forEach { line ->
            val trimmed = line.trim()

            if (trimmed.startsWith("#")) {
                if (currentLines.isNotEmpty()) {
                    result.add(
                        Section(
                            title = currentTitle,
                            text = currentLines.joinToString("\n")
                        )
                    )
                    currentLines.clear()
                }

                currentTitle = trimmed
                    .removePrefix("#")
                    .trim()
                    .ifBlank { document.title }

                currentLines.add(line)
            } else {
                currentLines.add(line)
            }
        }

        if (currentLines.isNotEmpty()) {
            result.add(
                Section(
                    title = currentTitle,
                    text = currentLines.joinToString("\n")
                )
            )
        }

        return result
    }

    private fun String.toWords(): List<String> {
        return split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private data class Section(
        val title: String,
        val text: String
    )
}
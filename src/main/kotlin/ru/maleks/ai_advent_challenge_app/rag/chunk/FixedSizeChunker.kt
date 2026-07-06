package ru.maleks.ai_advent_challenge_app.rag.chunk

import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentChunk

class FixedSizeChunker(
    private val chunkSizeWords: Int = 220,
    private val overlapWords: Int = 40
) : ChunkingStrategy {

    override val name: String = "fixed-size-${chunkSizeWords}-overlap-$overlapWords"

    override fun chunk(documents: List<RawDocument>): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()

        documents.forEach { document ->
            val words = document.text.toWords()

            if (words.isEmpty()) {
                return@forEach
            }

            var start = 0
            var localChunkNumber = 1

            while (start < words.size) {
                val end = minOf(start + chunkSizeWords, words.size)
                val chunkWords = words.subList(start, end)

                chunks.add(
                    DocumentChunk(
                        chunkId = "${document.source}::$name::$localChunkNumber",
                        source = document.source,
                        title = document.title,
                        section = "fixed-size",
                        strategy = name,
                        text = chunkWords.joinToString(" "),
                        startWord = start,
                        endWord = end
                    )
                )

                if (end == words.size) {
                    break
                }

                start = (end - overlapWords).coerceAtLeast(start + 1)
                localChunkNumber++
            }
        }

        return chunks
    }

    private fun String.toWords(): List<String> {
        return split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
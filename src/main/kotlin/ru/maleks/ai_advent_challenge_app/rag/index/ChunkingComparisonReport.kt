package ru.maleks.ai_advent_challenge_app.rag.index

import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex

object ChunkingComparisonReport {

    fun build(first: DocumentIndex, second: DocumentIndex): String {
        return """
            # Chunking comparison

            ## Strategy 1: ${first.strategy}

            - documents: ${first.stats.documents}
            - chunks: ${first.stats.chunks}
            - total words: ${first.stats.totalWords}
            - average chunk words: ${"%.2f".format(first.stats.averageChunkWords)}
            - min chunk words: ${first.stats.minChunkWords}
            - max chunk words: ${first.stats.maxChunkWords}
            - embedding model: ${first.embeddingModel}

            ## Strategy 2: ${second.strategy}

            - documents: ${second.stats.documents}
            - chunks: ${second.stats.chunks}
            - total words: ${second.stats.totalWords}
            - average chunk words: ${"%.2f".format(second.stats.averageChunkWords)}
            - min chunk words: ${second.stats.minChunkWords}
            - max chunk words: ${second.stats.maxChunkWords}
            - embedding model: ${second.embeddingModel}

            ## Practical interpretation

            Fixed-size chunking is predictable and easy to implement. It usually produces stable chunk sizes, but may cut text in the middle of a logical section.

            Structure-based chunking keeps headings and sections together. It is usually better for Markdown, articles and documentation, but chunk size may vary more.

            For RAG over documentation, structure-based chunking is often more readable. For mixed raw text or code, fixed-size chunking may be easier to control.
        """.trimIndent()
    }
}
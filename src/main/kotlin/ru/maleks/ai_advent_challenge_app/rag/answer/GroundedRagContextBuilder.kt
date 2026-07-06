package ru.maleks.ai_advent_challenge_app.rag.answer

import ru.maleks.ai_advent_challenge_app.rag.search.RerankedSearchResult

class GroundedRagContextBuilder(
    private val minBestScore: Double = 0.08,
    private val minSources: Int = 1,
    private val quoteMaxChars: Int = 450
) {

    fun build(
        question: String,
        results: List<RerankedSearchResult>
    ): GroundedRagContext {
        val bestScore = results.maxOfOrNull { it.finalScore } ?: 0.0

        if (results.size < minSources) {
            return GroundedRagContext(
                question = question,
                enoughContext = false,
                reason = "Not enough retrieved chunks. Required: $minSources, actual: ${results.size}.",
                sources = emptyList()
            )
        }

        if (bestScore < minBestScore) {
            return GroundedRagContext(
                question = question,
                enoughContext = false,
                reason = "Best relevance score is too low: ${"%.4f".format(bestScore)}. Threshold: $minBestScore.",
                sources = results.map { it.toSource() }
            )
        }

        return GroundedRagContext(
            question = question,
            enoughContext = true,
            reason = "Context is relevant enough. Best score: ${"%.4f".format(bestScore)}.",
            sources = results.map { it.toSource() }
        )
    }

    private fun RerankedSearchResult.toSource(): GroundedRagSource {
        return GroundedRagSource(
            source = chunk.source,
            title = chunk.title,
            section = chunk.section,
            chunkId = chunk.chunkId,
            quote = makeQuote(chunk.text),
            score = finalScore
        )
    }

    private fun makeQuote(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(quoteMaxChars)
    }
}
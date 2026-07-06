package ru.maleks.ai_advent_challenge_app.rag.search

import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex

class ImprovedRagRetriever(
    private val vectorSearchService: VectorSearchService,
    private val queryRewriter: QueryRewriter = QueryRewriter(),
    private val filter: SearchResultFilter = SearchResultFilter(minSimilarity = 0.08),
    private val reranker: HeuristicReranker = HeuristicReranker()
) {

    fun retrieve(
        question: String,
        index: DocumentIndex,
        searchTopK: Int = 8,
        finalTopK: Int = 3
    ): ImprovedRetrieveResult {
        val rewrittenQuery = queryRewriter.rewrite(question)

        val rawResults = vectorSearchService.search(
            question = rewrittenQuery,
            index = index,
            topK = searchTopK
        )

        val filteredResults = filter.filter(rawResults)

        val rerankedResults = reranker
            .rerank(
                originalQuestion = question,
                results = filteredResults
            )
            .take(finalTopK)

        return ImprovedRetrieveResult(
            originalQuestion = question,
            rewrittenQuery = rewrittenQuery,
            rawResults = rawResults,
            filteredResults = filteredResults,
            rerankedResults = rerankedResults
        )
    }
}
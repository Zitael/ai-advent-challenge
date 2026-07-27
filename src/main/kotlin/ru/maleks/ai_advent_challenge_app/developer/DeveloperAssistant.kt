package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContextBuilder
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import ru.maleks.ai_advent_challenge_app.prompt.ProjectRulesLoader
import ru.maleks.ai_advent_challenge_app.prompt.PromptContextAssembler

class DeveloperAssistant(
    private val ollamaClient: OllamaClient,
    private val index: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val gitProjectClient: GitProjectClient,
    private val codeReviewService: CodeReviewService,
    private val gitDiffProvider: GitDiffProvider,
    private val contextBuilder: GroundedRagContextBuilder = GroundedRagContextBuilder(
        minBestScore = 0.08,
        minSources = 1
    ),
    private val promptBuilder: DeveloperAssistantPromptBuilder =
        DeveloperAssistantPromptBuilder(),
    private val promptAssembler: PromptContextAssembler =
        PromptContextAssembler(
            ProjectRulesLoader()
        )
) {

    suspend fun answerProjectQuestion(question: String): String {
        val retrieveResult = retriever.retrieve(
            question = question,
            index = index,
            searchTopK = 10,
            finalTopK = 4
        )

        val groundedContext = contextBuilder.build(
            question = question,
            results = retrieveResult.rerankedResults
        )

        if (!groundedContext.enoughContext) {
            return """
                Не нашёл достаточно информации в README и папке docs.

                Причина: ${groundedContext.reason}

                Попробуй уточнить вопрос или добавить недостающую документацию в project/docs.
            """.trimIndent()
        }

        val branch = gitProjectClient.currentBranch()
        val prompt = promptAssembler.assemble(
            promptBuilder.build(
                question = question,
                branch = branch,
                groundedContext = groundedContext
            )
        )

        return ollamaClient.complete(prompt).answer
    }

    suspend fun reviewLocalChanges(): String {
        val changes = gitDiffProvider.localChanges()

        if (changes.changedFiles.isEmpty()) {
            return """
            Нет изменений для ревью.

            Git repository:
            ${changes.repositoryRoot}

            Проверь в этом каталоге:
            git status --short
        """.trimIndent()
        }

        println(
            "Git repository: ${changes.repositoryRoot}"
        )
        println(
            "Changed files: ${changes.changedFiles.size}"
        )
        println(
            "Diff size: ${changes.diff.length} characters"
        )

        return codeReviewService.review(
            diff = changes.diff,
            changedFiles = changes.changedFiles
        )
    }

    suspend fun currentBranch(): String = gitProjectClient.currentBranch()

    suspend fun status(): String = gitProjectClient.status()

    suspend fun diff(): String = gitProjectClient.diff()

    suspend fun files(): String = gitProjectClient.files()
}

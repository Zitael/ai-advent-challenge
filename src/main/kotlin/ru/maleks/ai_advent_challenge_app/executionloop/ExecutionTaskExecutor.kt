package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.Path

class ExecutionTaskExecutor(
    private val gatewayClient: ExecutionGatewayClient,
    private val fileTools: ProjectFileTools,
    private val fileChangeApplier: ExecutionFileChangeApplier,
    private val promptBuilder: ExecutionPromptBuilder = ExecutionPromptBuilder()
) {

    suspend fun execute(
        task: ExecutionTask,
        attemptNumber: Int,
        projectRoot: Path,
        previousFailure: String?
    ): ExecutionAgentResult {
        val contextSource = buildString {
            appendLine(readIfExists(Path.of("build.gradle.kts")))
            appendLine()
            appendLine(buildSearchContext(task))
            task.outputPath?.let { output ->
                appendLine()
                appendLine(readIfExists(output))
            }
        }

        val prompt = promptBuilder.buildExecutePrompt(
            task = task,
            attemptNumber = attemptNumber,
            projectContext = promptBuilder.buildProjectContext(
                projectRoot = projectRoot,
                sourceSnippet = contextSource
            ),
            previousFailure = previousFailure
        )

        val gatewayResult = gatewayClient.complete(
            prompt = prompt,
            purpose = ExecutionLlmPurpose.CODE_GENERATION,
            temperature = 0.1
        )

        if (gatewayResult.blocked) {
            return ExecutionAgentResult(
                llmAnswer = gatewayResult.answer,
                applyResult = FileApplyResult(
                    appliedFiles = emptyList(),
                    message = buildGatewayBlockedMessage(gatewayResult)
                ),
                gatewayResult = gatewayResult
            )
        }

        val applyResult = fileChangeApplier.apply(
            agentResponse = gatewayResult.answer,
            task = task
        )

        return ExecutionAgentResult(
            llmAnswer = gatewayResult.answer,
            applyResult = applyResult,
            gatewayResult = gatewayResult
        )
    }

    private fun buildGatewayBlockedMessage(result: ExecutionGatewayResult): String = buildString {
        append("Gateway blocked code generation. ")
        if (result.inputFindings.isNotEmpty()) {
            append("Input findings: ${result.inputFindings.joinToString(", ")}. ")
        }
        if (result.outputViolations.isNotEmpty()) {
            append("Output violations: ${result.outputViolations.joinToString(", ")}.")
        }
    }.trim()

    private fun readIfExists(relativePath: Path): String {
        return runCatching { fileTools.read(relativePath) }
            .getOrElse { "Unable to read $relativePath: ${it.message}" }
            .take(MAX_CONTEXT_CHARS)
    }

    private fun buildSearchContext(task: ExecutionTask): String {
        val terms = Regex("\\b[A-Z][A-Za-z0-9]{2,}\\b")
            .findAll(task.description)
            .map { it.value }
            .filterNot { it in IGNORED_TERMS }
            .distinct()
            .take(4)
            .toList()

        if (terms.isEmpty()) {
            return "Search context: no class-like terms extracted from task description."
        }

        return buildString {
            appendLine("Search context:")
            terms.forEach { term ->
                appendLine("- Term: $term")
                val matches = fileTools.search(term, limit = 8)
                if (matches.isEmpty()) {
                    appendLine("  no matches")
                } else {
                    matches.forEach { match ->
                        appendLine("  ${match.path}:${match.lineNumber} | ${match.line}")
                    }
                }
            }
        }.trim()
    }

    private companion object {
        const val MAX_CONTEXT_CHARS = 8_000

        val IGNORED_TERMS = setOf(
            "Markdown",
            "Gradle",
            "Kotlin",
            "Create",
            "Read",
            "Search",
            "The",
            "Use",
            "Cli"
        )
    }
}

data class ExecutionAgentResult(
    val llmAnswer: String,
    val applyResult: FileApplyResult,
    val gatewayResult: ExecutionGatewayResult? = null
) {
    val agentSummary: String
        get() = buildString {
            appendLine(applyResult.message)
            gatewayResult?.let { gateway ->
                appendLine()
                appendLine(
                    "Gateway: action=${gateway.inputGuardAction}, " +
                        "findings=${gateway.inputFindings}, blocked=${gateway.blocked}"
                )
            }
            appendLine()
            append(llmAnswer.take(MAX_SUMMARY_CHARS))
            if (llmAnswer.length > MAX_SUMMARY_CHARS) {
                appendLine()
                append("... [truncated]")
            }
        }.trim()

    private companion object {
        const val MAX_SUMMARY_CHARS = 4_000
    }
}

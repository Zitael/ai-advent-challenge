package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import ru.maleks.ai_advent_challenge_app.release.ReleaseProjectInspector
import kotlin.io.path.isRegularFile

class ExecutionTaskValidator(
    private val fileTools: ProjectFileTools,
    private val projectInspector: ReleaseProjectInspector = ReleaseProjectInspector(fileTools)
) {

    fun validate(
        task: ExecutionTask,
        agentResult: ExecutionAgentResult
    ): ExecutionValidationResult {
        if (agentResult.applyResult.appliedFiles.isEmpty()) {
            return ExecutionValidationResult(
                passed = false,
                message = "Validation failed: agent did not apply any file changes."
            )
        }

        val invariantScan = projectInspector.checkInvariants()
        val blockingFindings = invariantScan.blockers.filter { finding ->
            agentResult.applyResult.appliedFiles.any { path ->
                finding.path == path.toString().replace('\\', '/')
            }
        }

        if (blockingFindings.isNotEmpty()) {
            return ExecutionValidationResult(
                passed = false,
                message = buildString {
                    appendLine("Validation failed: blocking invariant violations in changed files.")
                    blockingFindings.forEach { finding ->
                        appendLine("- ${finding.rule}: ${finding.path}:${finding.line}")
                    }
                }.trim()
            )
        }

        return when (task.validation) {
            ExecutionValidationKind.FILE_EXISTS -> validateFileExists(task)
            ExecutionValidationKind.FILE_CONTAINS -> validateFileContains(task)
            ExecutionValidationKind.INVARIANTS -> validateInvariantsOnly(invariantScan.findings, agentResult)
        }
    }

    private fun validateFileExists(task: ExecutionTask): ExecutionValidationResult {
        val target = task.outputPath
            ?: return ExecutionValidationResult(
                passed = false,
                message = "Validation failed: task does not define output path."
            )

        val file = fileTools.root.resolve(target).normalize()
        if (!file.startsWith(fileTools.root)) {
            return ExecutionValidationResult(
                passed = false,
                message = "Validation failed: output path is outside project root: $target"
            )
        }

        if (!file.isRegularFile()) {
            return ExecutionValidationResult(
                passed = false,
                message = "Validation failed: expected file was not created: $target"
            )
        }

        return ExecutionValidationResult(
            passed = true,
            message = "Validation passed: file exists at ${target.toString().replace('\\', '/')}"
        )
    }

    private fun validateFileContains(task: ExecutionTask): ExecutionValidationResult {
        val existsResult = validateFileExists(task)
        if (!existsResult.passed) {
            return existsResult
        }

        val expected = task.expectedContent
            ?: return ExecutionValidationResult(
                passed = false,
                message = "Validation failed: expected content is not configured for task ${task.id}."
            )

        val target = checkNotNull(task.outputPath)
        val actual = fileTools.read(target)

        if (!actual.contains(expected)) {
            return ExecutionValidationResult(
                passed = false,
                message = "Validation failed: file ${target.toString().replace('\\', '/')} does not contain expected text: $expected"
            )
        }

        return ExecutionValidationResult(
            passed = true,
            message = "Validation passed: file contains expected content."
        )
    }

    private fun validateInvariantsOnly(
        findings: List<ru.maleks.ai_advent_challenge_app.release.InvariantFinding>,
        agentResult: ExecutionAgentResult
    ): ExecutionValidationResult {
        val relevant = findings.filter { finding ->
            agentResult.applyResult.appliedFiles.any { path ->
                finding.path == path.toString().replace('\\', '/')
            }
        }

        if (relevant.isEmpty()) {
            return ExecutionValidationResult(
                passed = true,
                message = "Validation passed: no invariant findings in changed files."
            )
        }

        return ExecutionValidationResult(
            passed = false,
            message = buildString {
                appendLine("Validation failed: invariant findings detected.")
                relevant.forEach { finding ->
                    appendLine("- ${finding.rule}: ${finding.path}:${finding.line}")
                }
            }.trim()
        )
    }
}

data class ExecutionValidationResult(
    val passed: Boolean,
    val message: String
)

package ru.maleks.ai_advent_challenge_app.release

import ru.maleks.ai_advent_challenge_app.developer.GitDiffProvider
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaOptimizationProfiles
import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReleaseAssistant(
    private val projectRoot: Path,
    private val ollamaClient: OllamaClient,
    private val gitDiffProvider: GitDiffProvider,
    private val fileTools: ProjectFileTools,
    private val commandRunner: ReleaseCommandRunner
) {
    private val inspector = ReleaseProjectInspector(fileTools)

    suspend fun prepare(config: ReleaseConfig): ReleasePipelineResult {
        val changes = gitDiffProvider.localChanges()
        val invariantScan = inspector.checkInvariants()
        val checks = if (config.runChecks) commandRunner.runChecks() else emptyList()

        val blockers = buildList {
            addAll(invariantScan.blockers.map {
                "${it.rule}: ${it.path}:${it.line}"
            })
            addAll(checks.filterNot { it.successful }.map {
                "Command failed: ${it.command}"
            })
        }

        val warnings = invariantScan.findings
            .filterNot { it.blocking }
            .map { "${it.rule}: ${it.path}:${it.line}" }

        Files.createDirectories(config.outputDirectory)

        val projectContext = inspector.inspectChangedFiles(changes.changedFiles)
        val aiReleaseNotes = generateReleaseNotes(
            version = config.version,
            changedFiles = changes.changedFiles,
            diff = changes.diff,
            projectContext = projectContext,
            blockers = blockers,
            warnings = warnings
        )
        val aiReview = generateReleaseReview(
            changedFiles = changes.changedFiles,
            diff = changes.diff,
            blockers = blockers,
            warnings = warnings
        )

        val releaseNotesPath = config.outputDirectory.resolve("RELEASE_NOTES.md")
        val changelogPath = config.outputDirectory.resolve("CHANGELOG.md")
        val reviewPath = config.outputDirectory.resolve("AI_REVIEW.md")
        val invariantsPath = config.outputDirectory.resolve("INVARIANTS.md")
        val reportPath = config.outputDirectory.resolve("RELEASE_REPORT.md")

        write(releaseNotesPath, aiReleaseNotes)
        write(changelogPath, buildChangelog(config.version, changes.changedFiles))
        write(reviewPath, aiReview)
        write(invariantsPath, buildInvariantReport(invariantScan))
        write(
            reportPath,
            buildReleaseReport(
                config = config,
                changedFiles = changes.changedFiles,
                blockers = blockers,
                warnings = warnings,
                checks = checks
            )
        )

        return ReleasePipelineResult(
            version = config.version,
            changedFiles = changes.changedFiles,
            blockers = blockers,
            warnings = warnings,
            artifacts = ReleaseArtifacts(
                outputDirectory = config.outputDirectory,
                releaseNotes = releaseNotesPath,
                changelog = changelogPath,
                review = reviewPath,
                invariants = invariantsPath,
                report = reportPath
            ),
            checks = checks
        )
    }

    private suspend fun generateReleaseNotes(
        version: String,
        changedFiles: List<String>,
        diff: String,
        projectContext: String,
        blockers: List<String>,
        warnings: List<String>
    ): String {
        val prompt = """
            Ты release manager и senior backend engineer.
            Подготовь пользовательские release notes на русском языке для версии $version.

            Требования:
            - опирайся только на переданные изменения;
            - отдели новые возможности, исправления и технические изменения;
            - не упоминай внутренние рассуждения модели;
            - если информации недостаточно, прямо скажи об этом;
            - формат — Markdown.

            Изменённые файлы:
            ${changedFiles.joinToString("\n") { "- $it" }}

            Блокеры:
            ${blockers.ifEmpty { listOf("Нет") }.joinToString("\n") { "- $it" }}

            Предупреждения:
            ${warnings.take(30).ifEmpty { listOf("Нет") }.joinToString("\n") { "- $it" }}

            Контекст изменённых файлов:
            $projectContext

            Git diff:
            ```diff
            ${diff.take(MAX_DIFF_CHARS)}
            ```
        """.trimIndent()

        return ollamaClient.complete(
            prompt = prompt,
            config = OllamaOptimizationProfiles.optimizedRag
        ).answer
    }

    private suspend fun generateReleaseReview(
        changedFiles: List<String>,
        diff: String,
        blockers: List<String>,
        warnings: List<String>
    ): String {
        val prompt = """
            Проведи финальную проверку готовности релиза как senior Kotlin/Java engineer.
            Ответь по-русски в Markdown.

            Разделы:
            # Резюме
            # Риски
            # Что проверить вручную
            # Решение о релизе

            Изменённые файлы:
            ${changedFiles.joinToString("\n") { "- $it" }}

            Автоматические блокеры:
            ${blockers.ifEmpty { listOf("Нет") }.joinToString("\n") { "- $it" }}

            Автоматические предупреждения:
            ${warnings.take(30).ifEmpty { listOf("Нет") }.joinToString("\n") { "- $it" }}

            Diff:
            ```diff
            ${diff.take(MAX_REVIEW_DIFF_CHARS)}
            ```
        """.trimIndent()

        return ollamaClient.complete(
            prompt = prompt,
            config = OllamaOptimizationProfiles.optimizedRag
        ).answer
    }

    private fun buildChangelog(version: String, changedFiles: List<String>): String = buildString {
        appendLine("# Changelog")
        appendLine()
        appendLine("## $version — ${LocalDateTime.now().format(DATE_FORMAT)}")
        appendLine()
        if (changedFiles.isEmpty()) {
            appendLine("No local changes detected.")
        } else {
            changedFiles.forEach { appendLine("- `$it`") }
        }
    }

    private fun buildInvariantReport(scan: InvariantScanResult): String = buildString {
        appendLine("# Invariant report")
        appendLine()
        appendLine("Checked files: ${scan.checkedFiles}")
        appendLine("Findings: ${scan.findings.size}")
        appendLine("Blocking findings: ${scan.blockers.size}")
        appendLine()
        if (scan.findings.isEmpty()) {
            appendLine("No violations found.")
        } else {
            scan.findings.take(300).forEach {
                val level = if (it.blocking) "BLOCKER" else "WARNING"
                appendLine("- **$level ${it.rule}** — `${it.path}:${it.line}` — `${it.fragment}`")
            }
        }
    }

    private fun buildReleaseReport(
        config: ReleaseConfig,
        changedFiles: List<String>,
        blockers: List<String>,
        warnings: List<String>,
        checks: List<CommandResult>
    ): String = buildString {
        appendLine("# Release report")
        appendLine()
        appendLine("- Version: `${config.version}`")
        appendLine("- Generated: `${LocalDateTime.now()}`")
        appendLine("- Changed files: `${changedFiles.size}`")
        appendLine("- Status: **${if (blockers.isEmpty()) "READY" else "BLOCKED"}**")
        appendLine()
        appendLine("## Blockers")
        appendLine()
        if (blockers.isEmpty()) appendLine("None.") else blockers.forEach { appendLine("- $it") }
        appendLine()
        appendLine("## Warnings")
        appendLine()
        if (warnings.isEmpty()) appendLine("None.") else warnings.take(100).forEach { appendLine("- $it") }
        appendLine()
        appendLine("## Automated checks")
        appendLine()
        if (checks.isEmpty()) {
            appendLine("Checks were skipped.")
        } else {
            checks.forEach { result ->
                appendLine("### `${result.command}`")
                appendLine()
                appendLine("Status: **${if (result.successful) "PASSED" else "FAILED"}**")
                appendLine()
                appendLine("```text")
                appendLine(result.output.ifBlank { "No output" })
                appendLine("```")
                appendLine()
            }
        }
        appendLine("## Generated artifacts")
        appendLine()
        appendLine("- `RELEASE_NOTES.md`")
        appendLine("- `CHANGELOG.md`")
        appendLine("- `AI_REVIEW.md`")
        appendLine("- `INVARIANTS.md`")
        appendLine("- `RELEASE_REPORT.md`")
    }

    private fun write(path: Path, content: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, content.trim() + "\n", StandardCharsets.UTF_8)
    }

    private companion object {
        const val MAX_DIFF_CHARS = 28_000
        const val MAX_REVIEW_DIFF_CHARS = 24_000
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}

package ru.maleks.ai_advent_challenge_app.release

import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.InvalidPathException
import java.nio.file.Path

class ReleaseProjectInspector(
    private val tools: ProjectFileTools
) {
    fun inspectChangedFiles(changedFiles: List<String>): String {
        if (changedFiles.isEmpty()) {
            return "No changed files."
        }

        return buildString {
            changedFiles
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .mapNotNull(::safeRelativePath)
                .take(MAX_FILES)
                .forEach { path ->
                    val relativePath = path.toString().replace('\\', '/')
                    appendLine("## $relativePath")
                    appendLine()

                    val content = runCatching { tools.read(path) }
                        .getOrElse { exception ->
                            appendLine("Unable to read: ${exception.message}")
                            appendLine()
                            return@forEach
                        }

                    appendLine("```")
                    appendLine(content.take(MAX_FILE_CHARS))
                    if (content.length > MAX_FILE_CHARS) {
                        appendLine("... [truncated]")
                    }
                    appendLine("```")
                    appendLine()
                }
        }.trim()
    }

    fun checkInvariants(): InvariantScanResult {
        val findings = mutableListOf<InvariantFinding>()
        val files = tools.searchableFiles()

        for (file in files) {
            val relative = tools.relative(file)
            val content = runCatching { tools.read(relative) }.getOrNull() ?: continue

            content.lineSequence().forEachIndexed { index, line ->
                RULES.forEach { rule ->
                    if (rule.regex.containsMatchIn(line)) {
                        findings += InvariantFinding(
                            rule = rule.name,
                            path = relative.toString().replace('\\', '/'),
                            line = index + 1,
                            fragment = line.trim().take(240),
                            blocking = rule.blocking
                        )
                    }
                }
            }
        }

        return InvariantScanResult(
            checkedFiles = files.size,
            findings = findings
        )
    }

    private fun safeRelativePath(value: String): Path? {
        if (value.startsWith("warning:", ignoreCase = true) ||
            value.startsWith("error:", ignoreCase = true) ||
            value.startsWith("fatal:", ignoreCase = true)
        ) {
            return null
        }

        return try {
            Path.of(value)
        } catch (_: InvalidPathException) {
            null
        }
    }

    private data class Rule(
        val name: String,
        val regex: Regex,
        val blocking: Boolean
    )

    private companion object {
        const val MAX_FILES = 25
        const val MAX_FILE_CHARS = 4_000

        val RULES = listOf(
            Rule("TODO", Regex("\\bTODO\\b"), blocking = false),
            Rule("FIXME", Regex("\\bFIXME\\b"), blocking = false),
            Rule("println", Regex("\\bprintln\\s*\\("), blocking = false),
            Rule(
                "hardcoded secret",
                Regex("(?i)(password|api[_-]?key|secret)\\s*[=:]\\s*[\"'][^\"']+[\"']"),
                blocking = true
            )
        )
    }
}

data class InvariantFinding(
    val rule: String,
    val path: String,
    val line: Int,
    val fragment: String,
    val blocking: Boolean
)

data class InvariantScanResult(
    val checkedFiles: Int,
    val findings: List<InvariantFinding>
) {
    val blockers: List<InvariantFinding>
        get() = findings.filter { it.blocking }
}

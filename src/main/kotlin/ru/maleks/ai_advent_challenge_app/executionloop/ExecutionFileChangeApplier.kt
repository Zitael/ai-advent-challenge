package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.Path

class ExecutionFileChangeApplier(
    private val fileTools: ProjectFileTools
) {
    fun apply(
        agentResponse: String,
        task: ExecutionTask
    ): FileApplyResult {
        val blocks = parseFileBlocks(agentResponse)
        if (blocks.isNotEmpty()) {
            return writeBlocks(blocks)
        }

        val fallbackPath = task.outputPath ?: return emptyResult()
        val fallbackContent = synthesizeMarkdown(task, agentResponse)
        if (fallbackContent.isBlank()) {
            return emptyResult()
        }

        fileTools.write(fallbackPath, fallbackContent)
        return FileApplyResult(
            appliedFiles = listOf(fallbackPath),
            message = "Fallback applied 1 file change: ${fallbackPath.toString().replace('\\', '/')}"
        )
    }

    private fun writeBlocks(blocks: List<Pair<Path, String>>): FileApplyResult {
        val appliedFiles = blocks.map { (relativePath, content) ->
            fileTools.write(relativePath, content.trimEnd())
            relativePath
        }

        return FileApplyResult(
            appliedFiles = appliedFiles,
            message = "Applied ${appliedFiles.size} file change(s): ${appliedFiles.joinToString { it.toString().replace('\\', '/') }}"
        )
    }

    private fun synthesizeMarkdown(
        task: ExecutionTask,
        agentResponse: String
    ): String {
        val body = agentResponse
            .substringBefore("Summary:", agentResponse)
            .trim()
            .ifBlank { agentResponse.trim() }

        val heading = task.expectedContent?.trim().orEmpty()
        val content = when {
            heading.isNotBlank() && body.contains(heading) -> body
            heading.isNotBlank() -> "$heading\n\n$body"
            else -> body
        }

        return content.trim()
    }

    private fun parseFileBlocks(agentResponse: String): List<Pair<Path, String>> {
        val explicitBlocks = FILE_BLOCK_WITH_END_REGEX.findAll(agentResponse)
            .map { match -> Path.of(match.groupValues[1].trim()) to match.groupValues[2] }
            .toList()

        if (explicitBlocks.isNotEmpty()) {
            return explicitBlocks
        }

        return FILE_BLOCK_WITHOUT_END_REGEX.findAll(agentResponse)
            .map { match -> Path.of(match.groupValues[1].trim()) to match.groupValues[2] }
            .toList()
    }

    private fun emptyResult(): FileApplyResult = FileApplyResult(
        appliedFiles = emptyList(),
        message = "No file blocks found in agent response. Expected format:\n===FILE: relative/path===\ncontent\n===END==="
    )

    private companion object {
        val FILE_BLOCK_WITH_END_REGEX = Regex(
            pattern = """===FILE:\s*(.+?)\s*===\s*\R(.*?)\s*===END===""",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

        val FILE_BLOCK_WITHOUT_END_REGEX = Regex(
            pattern = """===FILE:\s*(.+?)\s*===\s*\R(.*?)(?=\s*(?:===FILE:|Summary:|\z))""",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )
    }
}

data class FileApplyResult(
    val appliedFiles: List<Path>,
    val message: String
)

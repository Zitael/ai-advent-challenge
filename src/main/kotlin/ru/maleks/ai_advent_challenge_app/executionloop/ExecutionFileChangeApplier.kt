package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.Path

class ExecutionFileChangeApplier(
    private val fileTools: ProjectFileTools
) {
    fun apply(agentResponse: String): FileApplyResult {
        val blocks = FILE_BLOCK_REGEX.findAll(agentResponse).toList()

        if (blocks.isEmpty()) {
            return FileApplyResult(
                appliedFiles = emptyList(),
                message = "No file blocks found in agent response. Expected format:\n===FILE: relative/path===\ncontent\n===END==="
            )
        }

        val appliedFiles = mutableListOf<Path>()

        for (block in blocks) {
            val relativePath = Path.of(block.groupValues[1].trim())
            val content = block.groupValues[2].trimEnd()

            fileTools.write(relativePath, content)
            appliedFiles += relativePath
        }

        return FileApplyResult(
            appliedFiles = appliedFiles,
            message = "Applied ${appliedFiles.size} file change(s): ${appliedFiles.joinToString { it.toString().replace('\\', '/') }}"
        )
    }

    private companion object {
        val FILE_BLOCK_REGEX = Regex(
            pattern = """===FILE:\s*(.+?)\s*===\s*\R(.*?)\s*===END===""",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )
    }
}

data class FileApplyResult(
    val appliedFiles: List<Path>,
    val message: String
)

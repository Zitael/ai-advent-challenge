package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class ExecutionTaskQueueLoader {

    fun load(queueFile: Path): List<ExecutionTask> {
        require(queueFile.isRegularFile()) {
            "Task queue file not found: $queueFile"
        }

        val lines = Files.readAllLines(queueFile, StandardCharsets.UTF_8)
        val tasks = mutableListOf<ExecutionTask>()
        var current = mutableMapOf<String, String>()

        fun flushTask() {
            if (current.isEmpty()) {
                return
            }

            tasks += parseTask(current)
            current = mutableMapOf()
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) {
                continue
            }

            if (line.startsWith("id:", ignoreCase = true)) {
                flushTask()
                current["id"] = line.substringAfter(":").trim()
                continue
            }

            val separatorIndex = line.indexOf(':')
            if (separatorIndex <= 0) {
                continue
            }

            val key = line.substring(0, separatorIndex).trim().lowercase()
            val value = line.substring(separatorIndex + 1).trim()
            current[key] = value
        }

        flushTask()

        require(tasks.isNotEmpty()) {
            "Task queue is empty: $queueFile"
        }

        return tasks
    }

    private fun parseTask(fields: Map<String, String>): ExecutionTask {
        val id = fields["id"]?.takeIf { it.isNotBlank() }
            ?: error("Task entry is missing id")

        val description = fields["description"]?.takeIf { it.isNotBlank() }
            ?: error("Task $id is missing description")

        val output = fields["output"]?.takeIf { it.isNotBlank() }?.let(Path::of)
        val validation = when (fields["validation"]?.lowercase()) {
            "file-exists", "file_exists" -> ExecutionValidationKind.FILE_EXISTS
            "file-contains", "file_contains" -> ExecutionValidationKind.FILE_CONTAINS
            "invariants" -> ExecutionValidationKind.INVARIANTS
            else -> ExecutionValidationKind.FILE_EXISTS
        }

        return ExecutionTask(
            id = id,
            description = description,
            outputPath = output,
            validation = validation,
            expectedContent = fields["expected"]?.takeIf { it.isNotBlank() }
        )
    }
}

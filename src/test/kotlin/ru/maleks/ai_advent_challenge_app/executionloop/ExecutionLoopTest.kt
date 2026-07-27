package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionTaskQueueLoaderTest {

    private val loader = ExecutionTaskQueueLoader()

    @Test
    fun `loads structured tasks from markdown file`() {
        val file = Files.createTempFile("task-pool", ".md")
        Files.writeString(
            file,
            """
            id: task-001
            type: documentation
            profile: documentation
            description: Create docs/test.md
            output: execution-loop/artifacts/test.md
            validation: file-contains
            expected: Hello

            id: task-002
            type: research
            profile: research
            description: Create docs/other.md
            output: execution-loop/artifacts/other.md
            validation: file-exists
            """.trimIndent(),
            StandardCharsets.UTF_8
        )

        val tasks = loader.load(file)

        assertEquals(2, tasks.size)
        assertEquals("task-001", tasks[0].id)
        assertEquals(ExecutionValidationKind.FILE_CONTAINS, tasks[0].validation)
        assertEquals("Hello", tasks[0].expectedContent)
        assertEquals("task-002", tasks[1].id)
        assertEquals(ExecutionTaskType.RESEARCH, tasks[1].type)
    }
}

class ExecutionFileChangeApplierTest {

    @Test
    fun `applies file blocks with explicit end marker`() {
        val root = Files.createTempDirectory("execution-loop-test")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val applier = ExecutionFileChangeApplier(fileTools)

        val result = applier.apply(
            agentResponse = """
            ===FILE: execution-loop/artifacts/sample.md===
            # Sample
            content
            ===END===
            """.trimIndent(),
            task = sampleTask()
        )

        assertEquals(1, result.appliedFiles.size)
        assertTrue(
            fileTools.read(java.nio.file.Path.of("execution-loop/artifacts/sample.md"))
                .contains("# Sample")
        )
    }

    @Test
    fun `applies file blocks without end marker`() {
        val root = Files.createTempDirectory("execution-loop-no-end")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val applier = ExecutionFileChangeApplier(fileTools)

        val result = applier.apply(
            agentResponse = """
            ===FILE: execution-loop/artifacts/gradle-tasks.md===
            # Gradle Tasks
            - runExecutionLoop
            Summary: done
            """.trimIndent(),
            task = sampleTask(
                output = java.nio.file.Path.of("execution-loop/artifacts/gradle-tasks.md"),
                expected = "# Gradle Tasks"
            )
        )

        assertEquals(1, result.appliedFiles.size)
        assertTrue(
            fileTools.read(java.nio.file.Path.of("execution-loop/artifacts/gradle-tasks.md"))
                .contains("# Gradle Tasks")
        )
    }

    @Test
    fun `falls back to synthesized markdown when research profile returns text only`() {
        val root = Files.createTempDirectory("execution-loop-fallback")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val applier = ExecutionFileChangeApplier(fileTools)

        val result = applier.apply(
            agentResponse = """
            ## Краткий ответ
            TaskStateStorage writes task-state.json using Jackson.

            ## Ключевые файлы
            - src/main/kotlin/.../TaskStateStorage.kt
            """.trimIndent(),
            task = sampleTask(
                output = java.nio.file.Path.of("execution-loop/artifacts/state-storage.md"),
                expected = "# Task State Storage"
            )
        )

        assertEquals(1, result.appliedFiles.size)
        assertTrue(result.message.contains("Fallback applied"))
        val content = fileTools.read(java.nio.file.Path.of("execution-loop/artifacts/state-storage.md"))
        assertTrue(content.startsWith("# Task State Storage"))
        assertTrue(content.contains("TaskStateStorage"))
    }

    @Test
    fun `returns empty result when no file blocks and no output path`() {
        val root = Files.createTempDirectory("execution-loop-empty")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val applier = ExecutionFileChangeApplier(fileTools)

        val result = applier.apply(
            agentResponse = "No files here",
            task = sampleTask(output = null)
        )

        assertTrue(result.appliedFiles.isEmpty())
        assertTrue(result.message.contains("No file blocks"))
    }

    private fun sampleTask(
        output: java.nio.file.Path? = java.nio.file.Path.of("execution-loop/artifacts/sample.md"),
        expected: String? = null
    ): ExecutionTask = ExecutionTask(
        id = "task-001",
        description = "sample",
        type = ExecutionTaskType.DOCUMENTATION,
        profile = ExecutionTaskProfile.DOCUMENTATION,
        outputPath = output,
        validation = ExecutionValidationKind.FILE_CONTAINS,
        expectedContent = expected
    )
}

class ExecutionTaskValidatorTest {

    @Test
    fun `passes when expected output file exists and contains text`() {
        val root = Files.createTempDirectory("execution-loop-validator")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val validator = ExecutionTaskValidator(
            fileTools = fileTools,
            commandRunner = ru.maleks.ai_advent_challenge_app.release.ReleaseCommandRunner(root)
        )

        fileTools.write(
            java.nio.file.Path.of("execution-loop/artifacts/report.md"),
            "# Gradle Tasks\n- runExecutionLoop"
        )

        val result = validator.validate(
            task = ExecutionTask(
                id = "task-001",
                description = "Create report",
                type = ExecutionTaskType.DOCUMENTATION,
                profile = ExecutionTaskProfile.DOCUMENTATION,
                outputPath = java.nio.file.Path.of("execution-loop/artifacts/report.md"),
                validation = ExecutionValidationKind.FILE_CONTAINS,
                expectedContent = "# Gradle Tasks"
            ),
            agentResult = ExecutionAgentResult(
                llmAnswer = "done",
                applyResult = FileApplyResult(
                    appliedFiles = listOf(java.nio.file.Path.of("execution-loop/artifacts/report.md")),
                    message = "applied"
                )
            )
        )

        assertTrue(result.passed)
    }

    @Test
    fun `fails when agent did not apply files`() {
        val root = Files.createTempDirectory("execution-loop-validator-fail")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val validator = ExecutionTaskValidator(
            fileTools = fileTools,
            commandRunner = ru.maleks.ai_advent_challenge_app.release.ReleaseCommandRunner(root)
        )

        val result = validator.validate(
            task = ExecutionTask(
                id = "task-001",
                description = "Create report",
                type = ExecutionTaskType.DOCUMENTATION,
                profile = ExecutionTaskProfile.DOCUMENTATION,
                outputPath = java.nio.file.Path.of("execution-loop/artifacts/report.md"),
                validation = ExecutionValidationKind.FILE_EXISTS
            ),
            agentResult = ExecutionAgentResult(
                llmAnswer = "done",
                applyResult = FileApplyResult(
                    appliedFiles = emptyList(),
                    message = "none"
                )
            )
        )

        assertFalse(result.passed)
        assertEquals(ExecutionFailureCategory.NO_FILE_CHANGES, result.category)
    }
}

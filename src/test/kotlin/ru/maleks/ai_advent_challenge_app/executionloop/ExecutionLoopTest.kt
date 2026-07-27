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
            # Task pool

            id: task-001
            description: Create docs/test.md
            output: build/execution-loop/test.md
            validation: file-contains
            expected: Hello

            id: task-002
            description: Create docs/other.md
            output: build/execution-loop/other.md
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
        assertEquals(ExecutionValidationKind.FILE_EXISTS, tasks[1].validation)
    }
}

class ExecutionFileChangeApplierTest {

    @Test
    fun `applies file blocks from agent response`() {
        val root = Files.createTempDirectory("execution-loop-test")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val applier = ExecutionFileChangeApplier(fileTools)

        val result = applier.apply(
            """
            ===FILE: build/execution-loop/sample.md===
            # Sample
            content
            ===END===
            """.trimIndent()
        )

        assertEquals(1, result.appliedFiles.size)
        assertTrue(fileTools.read(java.nio.file.Path.of("build/execution-loop/sample.md")).contains("# Sample"))
    }

    @Test
    fun `returns empty result when no file blocks present`() {
        val root = Files.createTempDirectory("execution-loop-empty")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val applier = ExecutionFileChangeApplier(fileTools)

        val result = applier.apply("No files here")

        assertTrue(result.appliedFiles.isEmpty())
        assertTrue(result.message.contains("No file blocks"))
    }
}

class ExecutionTaskValidatorTest {

    @Test
    fun `passes when expected output file exists and contains text`() {
        val root = Files.createTempDirectory("execution-loop-validator")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val validator = ExecutionTaskValidator(fileTools)

        fileTools.write(
            java.nio.file.Path.of("build/execution-loop/report.md"),
            "# Gradle Tasks\n- runExecutionLoop"
        )

        val result = validator.validate(
            task = ExecutionTask(
                id = "task-001",
                description = "Create report",
                outputPath = java.nio.file.Path.of("build/execution-loop/report.md"),
                validation = ExecutionValidationKind.FILE_CONTAINS,
                expectedContent = "# Gradle Tasks"
            ),
            agentResult = ExecutionAgentResult(
                llmAnswer = "done",
                applyResult = FileApplyResult(
                    appliedFiles = listOf(java.nio.file.Path.of("build/execution-loop/report.md")),
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
        val validator = ExecutionTaskValidator(fileTools)

        val result = validator.validate(
            task = ExecutionTask(
                id = "task-001",
                description = "Create report",
                outputPath = java.nio.file.Path.of("build/execution-loop/report.md"),
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
    }
}

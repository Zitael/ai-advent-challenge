package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.prompt.PromptContextAssembler
import java.nio.file.Path

class ExecutionPromptBuilder(
    private val promptAssembler: PromptContextAssembler = PromptContextAssembler()
) {

    fun buildExecutePrompt(
        task: ExecutionTask,
        attemptNumber: Int,
        projectContext: String,
        previousFailure: String?
    ): String {
        val retrySection = previousFailure?.let { failure ->
            """
                Previous attempt failed validation:
                $failure

                Fix the issue and produce corrected file output.
            """.trimIndent()
        } ?: "This is the first attempt."

        val outputInstruction = task.outputPath?.let { path ->
            """
                Required output file:
                - ${path.toString().replace('\\', '/')}
            """.trimIndent()
        } ?: """
            Create or update only files under build/execution-loop/.
        """.trimIndent()

        val prompt = """
            You are executing one task inside a controlled execution loop for a Kotlin/Gradle project.

            Task id: ${task.id}
            Attempt: $attemptNumber
            Task:
            ${task.description}

            $outputInstruction

            Rules:
            - Work only inside the project repository.
            - Do not delete existing project files.
            - Do not run git commands.
            - Do not invent APIs or files that do not exist unless the task explicitly requires creating a new file.
            - Use Kotlin for any code examples unless the task explicitly asks for Markdown documentation.
            - For every file you create or update, use this exact format:

            ===FILE: relative/path/from/project/root===
            file content
            ===END===

            $retrySection

            Project context:
            $projectContext

            Provide the file blocks first, then a short summary under heading "Summary:".
        """.trimIndent()

        return promptAssembler.assemble(prompt)
    }

    fun buildProjectContext(
        projectRoot: Path,
        sourceSnippet: String
    ): String = """
        Project root: ${projectRoot.toAbsolutePath().normalize()}
        Relevant source excerpt:
        $sourceSnippet
    """.trimIndent()
}

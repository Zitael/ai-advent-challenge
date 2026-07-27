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
            Create or update only files under execution-loop/artifacts/ unless the task explicitly names another safe path.
        """.trimIndent()

        val prompt = """
            You are executing one task inside a controlled execution loop for a Kotlin/Gradle project.

            Selected profile: ${task.profile.name}
            Task type: ${task.type.name}
            Task id: ${task.id}
            Attempt: $attemptNumber

            Profile guidance:
            ${task.profile.instruction()}

            Task:
            ${task.description}

            $outputInstruction

            Mandatory deliverable rules:
            - This is an EXECUTION task, not a chat-only research answer.
            - You MUST create or update the required output file.
            - Put analysis inside the output file, not only in chat text.
            - Work only inside the project repository.
            - Do not delete existing project files.
            - Do not run git commands.
            - Do not invent APIs or files that do not exist unless the task explicitly requires creating a new file.
            - Use Kotlin for code unless the task explicitly asks for Markdown documentation.
            - For every file you create or update, use this exact format:

            ===FILE: relative/path/from/project/root===
            file content
            ===END===

            If you cannot add ===END===, stop file content before the Summary section.

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

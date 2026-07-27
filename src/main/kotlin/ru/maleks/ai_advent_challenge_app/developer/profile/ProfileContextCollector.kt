package ru.maleks.ai_advent_challenge_app.developer.profile

import ru.maleks.ai_advent_challenge_app.rag.answer.GroundedRagContextBuilder
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import ru.maleks.ai_advent_challenge_app.rag.search.ImprovedRagRetriever
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.relativeTo

class ProfileContextCollector(
    private val projectRoot: Path,
    private val documentationIndex: DocumentIndex,
    private val codeIndex: DocumentIndex,
    private val retriever: ImprovedRagRetriever,
    private val contextBuilder: GroundedRagContextBuilder = GroundedRagContextBuilder(
        minBestScore = 0.0,
        minSources = 1
    )
) {
    fun collect(task: String, profile: AssistantProfile): ProfileContext {
        val documentation = retrieve(task, documentationIndex, finalTopK = 4)
        val code = retrieve(task, codeIndex, finalTopK = 8)

        return ProfileContext(
            gitStatus = runCommand(listOf("git", "status", "--short", "--branch")),
            dependencies = readDependencies(),
            relevantDocumentation = documentation,
            relevantCode = code,
            recentLogs = readRecentLogs(),
            testResult = if (profile.runTests) runTests() else "Tests are not required for this profile."
        )
    }

    private fun retrieve(
        task: String,
        index: DocumentIndex,
        finalTopK: Int
    ): String {
        if (index.chunks.isEmpty()) return "No indexed sources."

        val result = retriever.retrieve(
            question = task,
            index = index,
            searchTopK = maxOf(finalTopK * 2, 10),
            finalTopK = finalTopK
        )

        val context = contextBuilder.build(task, result.rerankedResults)
        if (context.sources.isEmpty()) return "No relevant sources found."

        return context.sources.joinToString("\n\n") { source ->
            """
                [SOURCE]
                file: ${source.source}
                section: ${source.section}
                relevance: ${"%.4f".format(source.score)}
                content:
                ${source.quote}
                [/SOURCE]
            """.trimIndent()
        }
    }

    private fun readDependencies(): String {
        val candidates = listOf(
            projectRoot.resolve("build.gradle.kts"),
            projectRoot.resolve("settings.gradle.kts"),
            projectRoot.resolve("gradle.properties")
        )

        return candidates
            .filter(Files::isRegularFile)
            .joinToString("\n\n") { path ->
                "## ${path.relativeTo(projectRoot)}\n${Files.readString(path).take(MAX_FILE_CHARS)}"
            }
            .ifBlank { "No Gradle configuration found." }
    }

    private fun readRecentLogs(): String {
        val allowedExtensions = setOf("log", "txt", "md")
        val candidates = Files.walk(projectRoot).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter { it.extension.lowercase() in allowedExtensions }
                .filter { path ->
                    path.relativeTo(projectRoot).none { part ->
                        part.toString() in IGNORED_DIRECTORIES
                    }
                }
                .sorted { first, second ->
                    Files.getLastModifiedTime(second).compareTo(Files.getLastModifiedTime(first))
                }
                .limit(3)
                .toList()
        }

        return candidates.joinToString("\n\n") { path ->
            val content = runCatching { Files.readString(path) }.getOrDefault("")
            "## ${path.relativeTo(projectRoot)}\n${content.takeLast(MAX_LOG_CHARS)}"
        }.ifBlank { "No readable project logs found." }
    }

    private fun runTests(): String {
        val wrapper = if (System.getProperty("os.name").lowercase().contains("win")) {
            projectRoot.resolve("gradlew.bat").toString()
        } else {
            projectRoot.resolve("gradlew").toString()
        }

        if (!Files.exists(Path.of(wrapper))) {
            return "Tests were not run: Gradle wrapper was not found."
        }

        return runCommand(listOf(wrapper, "test", "--console=plain"), timeoutSeconds = 180)
    }

    private fun runCommand(
        command: List<String>,
        timeoutSeconds: Long = 20
    ): String {
        return runCatching {
            val process = ProcessBuilder(command)
                .directory(projectRoot.toFile())
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@runCatching "Command timed out: ${command.joinToString(" ")}"
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            """
                command: ${command.joinToString(" ")}
                exit_code: ${process.exitValue()}
                output:
                ${output.takeLast(MAX_COMMAND_CHARS).ifBlank { "<empty>" }}
            """.trimIndent()
        }.getOrElse { exception ->
            "Command failed: ${command.joinToString(" ")}\n${exception.message}"
        }
    }

    private companion object {
        val IGNORED_DIRECTORIES = setOf(".git", ".gradle", ".idea", ".kotlin", "build")
        const val MAX_FILE_CHARS = 12_000
        const val MAX_LOG_CHARS = 5_000
        const val MAX_COMMAND_CHARS = 12_000
    }
}

data class ProfileContext(
    val gitStatus: String,
    val dependencies: String,
    val relevantDocumentation: String,
    val relevantCode: String,
    val recentLogs: String,
    val testResult: String
)

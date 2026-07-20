package ru.maleks.ai_advent_challenge_app.developer

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GitDiffProvider(
    projectPath: Path
) {

    private val repositoryRoot: Path =
        resolveRepositoryRoot(projectPath)

    fun repositoryRoot(): Path = repositoryRoot

    fun localChanges(): ReviewChanges {
        val unstagedDiff = runGit(
            "diff",
            "--no-ext-diff",
            "--binary"
        )

        val stagedDiff = runGit(
            "diff",
            "--cached",
            "--no-ext-diff",
            "--binary"
        )

        val unstagedFiles = gitFileList(
            "diff",
            "--name-only"
        )

        val stagedFiles = gitFileList(
            "diff",
            "--cached",
            "--name-only"
        )

        val untrackedFiles = gitFileList(
            "ls-files",
            "--others",
            "--exclude-standard"
        )

        val changedFiles = (
                unstagedFiles +
                        stagedFiles +
                        untrackedFiles
                )
            .distinct()
            .sorted()

        if (changedFiles.isEmpty()) {
            return ReviewChanges(
                diff = "",
                changedFiles = emptyList(),
                repositoryRoot = repositoryRoot
            )
        }

        val diff = buildString {
            if (unstagedDiff.isNotBlank()) {
                appendLine("## Unstaged changes")
                appendLine()
                appendLine(unstagedDiff)
                appendLine()
            }

            if (stagedDiff.isNotBlank()) {
                appendLine("## Staged changes")
                appendLine()
                appendLine(stagedDiff)
                appendLine()
            }

            val untrackedContent =
                buildUntrackedFilesContent(untrackedFiles)

            if (untrackedContent.isNotBlank()) {
                appendLine("## Untracked files")
                appendLine()
                appendLine(untrackedContent)
            }
        }.trim()

        return ReviewChanges(
            diff = diff,
            changedFiles = changedFiles,
            repositoryRoot = repositoryRoot
        )
    }

    fun pullRequestChanges(
        baseRef: String
    ): ReviewChanges {
        require(baseRef.isNotBlank()) {
            "Base ref must not be blank"
        }

        val normalizedBase = baseRef
            .removePrefix("refs/heads/")
            .removePrefix("origin/")

        runGit(
            "fetch",
            "origin",
            normalizedBase,
            "--depth=1"
        )

        val comparison =
            "origin/$normalizedBase...HEAD"

        val diff = runGit(
            "diff",
            "--no-ext-diff",
            "--binary",
            comparison
        )

        val changedFiles = gitFileList(
            "diff",
            "--name-only",
            comparison
        )
            .distinct()
            .sorted()

        return ReviewChanges(
            diff = diff,
            changedFiles = changedFiles,
            repositoryRoot = repositoryRoot
        )
    }

    private fun buildUntrackedFilesContent(
        untrackedFiles: List<String>
    ): String {
        return buildString {
            untrackedFiles.forEach { relativePath ->
                appendLine("### New file: $relativePath")
                appendLine()

                val file = repositoryRoot
                    .resolve(relativePath)
                    .normalize()

                if (!file.startsWith(repositoryRoot)) {
                    appendLine(
                        "[Skipped: path is outside repository]"
                    )
                    appendLine()
                    return@forEach
                }

                if (!Files.isRegularFile(file)) {
                    appendLine(
                        "[Skipped: not a regular file]"
                    )
                    appendLine()
                    return@forEach
                }

                val size = Files.size(file)

                if (size > MAX_UNTRACKED_FILE_SIZE_BYTES) {
                    appendLine(
                        "[Skipped: file is larger than " +
                                "$MAX_UNTRACKED_FILE_SIZE_BYTES bytes]"
                    )
                    appendLine()
                    return@forEach
                }

                if (!isReviewableTextFile(file)) {
                    appendLine(
                        "[Skipped: binary or unsupported file]"
                    )
                    appendLine()
                    return@forEach
                }

                val content = runCatching {
                    Files.readString(
                        file,
                        StandardCharsets.UTF_8
                    )
                }.getOrElse { exception ->
                    appendLine(
                        "[Failed to read file: " +
                                "${exception.message}]"
                    )
                    appendLine()
                    return@forEach
                }

                appendLine("```")
                appendLine(content)
                appendLine("```")
                appendLine()
            }
        }.trim()
    }

    private fun gitFileList(
        vararg args: String
    ): List<String> {
        return runGit(*args)
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
    }

    private fun isReviewableTextFile(
        file: Path
    ): Boolean {
        val fileName = file.fileName
            .toString()
            .lowercase()

        val extension = fileName.substringAfterLast(
            delimiter = '.',
            missingDelimiterValue = ""
        )

        return extension in REVIEWABLE_EXTENSIONS ||
                fileName in REVIEWABLE_FILE_NAMES
    }

    private fun runGit(
        vararg args: String
    ): String {
        return executeGit(
            workingDirectory = repositoryRoot,
            args = args
        )
    }

    private fun resolveRepositoryRoot(
        projectPath: Path
    ): Path {
        val normalizedPath = projectPath
            .toAbsolutePath()
            .normalize()

        require(Files.exists(normalizedPath)) {
            "Project path does not exist: $normalizedPath"
        }

        val result = executeGit(
            workingDirectory = normalizedPath,
            args = arrayOf(
                "rev-parse",
                "--show-toplevel"
            )
        )

        require(result.isNotBlank()) {
            "Git repository root was not found from: " +
                    normalizedPath
        }

        return Path.of(result)
            .toAbsolutePath()
            .normalize()
    }

    private fun executeGit(
        workingDirectory: Path,
        args: Array<out String>
    ): String {
        val command = buildList {
            add("git")
            add("--no-pager")
            addAll(args)
        }

        val process = ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .apply {
                environment()["GIT_PAGER"] = "cat"
                environment()["PAGER"] = "cat"
                environment()["GIT_TERMINAL_PROMPT"] = "0"
            }
            .start()

        val executor = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "git-output-reader").apply {
                isDaemon = true
            }
        }

        val stdoutFuture = executor.submit<String> {
            process.inputStream
                .bufferedReader()
                .use { reader -> reader.readText() }
        }

        val stderrFuture = executor.submit<String> {
            process.errorStream
                .bufferedReader()
                .use { reader -> reader.readText() }
        }

        try {
            val finished = process.waitFor(
                GIT_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )

            if (!finished) {
                process.destroy()

                if (
                    !process.waitFor(
                        DESTROY_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                    )
                ) {
                    process.destroyForcibly()
                }

                error(
                    "Git command timed out: " +
                        args.joinToString(" ")
                )
            }

            val stdout = stdoutFuture
                .get(
                    OUTPUT_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                )
                .trim()

            val stderr = stderrFuture
                .get(
                    OUTPUT_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                )
                .trim()

            check(process.exitValue() == 0) {
                buildString {
                    append("Git command failed")
                    append(" (exit code ")
                    append(process.exitValue())
                    append("): ")
                    appendLine(args.joinToString(" "))

                    if (stderr.isNotBlank()) {
                        appendLine(stderr)
                    }

                    if (stdout.isNotBlank()) {
                        append(stdout)
                    }
                }.trim()
            }

            return stdout
        } finally {
            executor.shutdownNow()
        }
    }

    private companion object {
        const val GIT_TIMEOUT_SECONDS = 120L
        const val OUTPUT_TIMEOUT_SECONDS = 30L
        const val DESTROY_TIMEOUT_SECONDS = 5L

        const val MAX_UNTRACKED_FILE_SIZE_BYTES =
            256 * 1024L

        val REVIEWABLE_EXTENSIONS = setOf(
            "kt",
            "kts",
            "java",
            "groovy",
            "gradle",
            "xml",
            "json",
            "yaml",
            "yml",
            "md",
            "adoc",
            "txt",
            "properties",
            "sql",
            "sh",
            "bat",
            "ps1",
            "js",
            "ts",
            "html",
            "css"
        )

        val REVIEWABLE_FILE_NAMES = setOf(
            "dockerfile",
            "makefile",
            ".gitignore",
            ".gitattributes"
        )
    }
}

data class ReviewChanges(
    val diff: String,
    val changedFiles: List<String>,
    val repositoryRoot: Path
)
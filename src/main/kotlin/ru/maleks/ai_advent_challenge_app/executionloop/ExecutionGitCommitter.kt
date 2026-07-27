package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ExecutionGitCommitter(
    private val projectRoot: Path
) {
    fun commit(
        files: List<Path>,
        message: String
    ): GitCommitResult {
        if (files.isEmpty()) {
            return GitCommitResult(
                committed = false,
                message = "No files to commit."
            )
        }

        val relativePaths = files.map { it.toString().replace('\\', '/') }

        val addResult = runGit(listOf("add", "--") + relativePaths)
        if (!addResult.successful) {
            return GitCommitResult(
                committed = false,
                message = "git add failed: ${addResult.output}"
            )
        }

        val staged = runGit(listOf("diff", "--cached", "--name-only"))
        val stagedPaths = staged.output
            .lineSequence()
            .map { it.trim().replace('\\', '/') }
            .filter { it.isNotBlank() }
            .toSet()

        val stagedTaskFiles = relativePaths.filter { it in stagedPaths }
        if (stagedTaskFiles.isEmpty()) {
            return GitCommitResult(
                committed = false,
                message = "Commit skipped: artifact unchanged (${relativePaths.joinToString()})."
            )
        }

        val commitResult = runGit(listOf("commit", "-m", message))
        return GitCommitResult(
            committed = commitResult.successful,
            message = if (commitResult.successful) {
                "Committed ${stagedTaskFiles.size} file(s): ${stagedTaskFiles.joinToString()}"
            } else {
                "git commit failed: ${commitResult.output}"
            }
        )
    }

    private fun runGit(args: List<String>): GitCommandResult {
        val command = listOf("git", "-C", projectRoot.toAbsolutePath().toString()) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "execution-loop-git").apply { isDaemon = true }
        }

        val outputFuture = executor.submit<String> {
            process.inputStream.bufferedReader().use { it.readText() }
        }

        return try {
            val finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                GitCommandResult(
                    successful = false,
                    output = "Git command timed out: ${args.joinToString(" ")}"
                )
            } else {
                GitCommandResult(
                    successful = process.exitValue() == 0,
                    output = outputFuture.get(10, TimeUnit.SECONDS).trim()
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private companion object {
        const val COMMAND_TIMEOUT_SECONDS = 60L
    }
}

data class GitCommitResult(
    val committed: Boolean,
    val message: String
)

private data class GitCommandResult(
    val successful: Boolean,
    val output: String
)

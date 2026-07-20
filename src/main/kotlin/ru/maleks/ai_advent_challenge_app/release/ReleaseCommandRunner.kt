package ru.maleks.ai_advent_challenge_app.release

import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ReleaseCommandRunner(
    private val projectRoot: Path
) {
    fun runChecks(): List<CommandResult> {
        val windows = System.getProperty("os.name").lowercase().contains("win")

        return listOf(
            run(gradleCommand(windows, "test", "--console=plain")),
            run(gradleCommand(windows, "build", "-x", "test", "--console=plain"))
        )
    }

    private fun gradleCommand(
        windows: Boolean,
        vararg args: String
    ): List<String> {
        return if (windows) {
            listOf("cmd", "/c", "gradlew.bat") + args
        } else {
            listOf("./gradlew") + args
        }
    }

    private fun run(command: List<String>): CommandResult {
        val process = ProcessBuilder(command)
            .directory(projectRoot.toFile())
            .redirectErrorStream(true)
            .start()

        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "release-command-output").apply { isDaemon = true }
        }

        val outputFuture = executor.submit<String> {
            process.inputStream.bufferedReader().use { it.readText() }
        }

        return try {
            val finished = process.waitFor(COMMAND_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            if (!finished) {
                process.destroyForcibly()
                CommandResult(
                    command = command.joinToString(" "),
                    exitCode = null,
                    output = outputFuture.get(10, TimeUnit.SECONDS).takeLast(MAX_OUTPUT_CHARS),
                    timedOut = true
                )
            } else {
                CommandResult(
                    command = command.joinToString(" "),
                    exitCode = process.exitValue(),
                    output = outputFuture.get(10, TimeUnit.SECONDS).takeLast(MAX_OUTPUT_CHARS),
                    timedOut = false
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private companion object {
        const val COMMAND_TIMEOUT_MINUTES = 8L
        const val MAX_OUTPUT_CHARS = 12_000
    }
}

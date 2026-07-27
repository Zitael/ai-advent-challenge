package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.file.Path

data class ExecutionLoopConfig(
    val projectRoot: Path,
    val queueFile: Path,
    val logDirectory: Path,
    val maxAttemptsPerTask: Int = 2,
    val ollamaBaseUrl: String,
    val ollamaModel: String,
    val taskLimit: Int? = null
) {
    init {
        require(maxAttemptsPerTask >= 1) {
            "maxAttemptsPerTask must be at least 1"
        }
    }
}

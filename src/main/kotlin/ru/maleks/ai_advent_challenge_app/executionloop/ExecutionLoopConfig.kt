package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.file.Path

data class ExecutionLoopConfig(
    val projectRoot: Path,
    val queueFile: Path,
    val logDirectory: Path,
    val metricsDirectory: Path,
    val maxAttemptsPerTask: Int = 2,
    val gatewayBaseUrl: String,
    val gatewayModel: String,
    val gatewayApiKey: String? = null,
    val securityReviewEnabled: Boolean = true,
    val runNumber: Int = 1,
    val taskLimit: Int? = null,
    val createCommits: Boolean = true
) {
    init {
        require(maxAttemptsPerTask >= 1) {
            "maxAttemptsPerTask must be at least 1"
        }
        require(runNumber >= 1) {
            "runNumber must be at least 1"
        }
    }
}

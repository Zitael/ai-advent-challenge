package ru.maleks.ai_advent_challenge_app.executionloop

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import ru.maleks.ai_advent_challenge_app.release.ReleaseCommandRunner
import java.nio.file.Files
import java.nio.file.Path

suspend fun main(args: Array<String>) {
    val dotenv = dotenv { ignoreIfMissing = true }

    val projectRoot = Path.of(
        dotenv["PROJECT_ROOT"]
            ?: System.getenv("PROJECT_ROOT")
            ?: "."
    ).toAbsolutePath().normalize()

    val gatewayBaseUrl = dotenv["GATEWAY_BASE_URL"]
        ?: System.getenv("GATEWAY_BASE_URL")
        ?: "http://127.0.0.1:8090"

    val gatewayModel = dotenv["GATEWAY_DEFAULT_MODEL"]
        ?: System.getenv("GATEWAY_DEFAULT_MODEL")
        ?: "openai/gpt-4o-mini"

    val gatewayApiKey = dotenv["GATEWAY_API_KEY"]
        ?: System.getenv("GATEWAY_API_KEY")

    val queueFile = resolveQueueFile(projectRoot, args)
    val taskLimit = readIntArg(args, "--limit=")
    val runNumber = readIntArg(args, "--run=") ?: 1
    val createCommits = !args.contains("--no-commit")
    val securityReviewEnabled = !args.contains("--no-security")

    val logDirectory = projectRoot.resolve("build/execution-loop/logs")
    val metricsDirectory = projectRoot.resolve("build/execution-loop/metrics")
    Files.createDirectories(logDirectory)
    Files.createDirectories(metricsDirectory)

    val config = ExecutionLoopConfig(
        projectRoot = projectRoot,
        queueFile = queueFile,
        logDirectory = logDirectory,
        metricsDirectory = metricsDirectory,
        maxAttemptsPerTask = if (securityReviewEnabled) 3 else 2,
        gatewayBaseUrl = gatewayBaseUrl.removeSuffix("/"),
        gatewayModel = gatewayModel,
        gatewayApiKey = gatewayApiKey,
        securityReviewEnabled = securityReviewEnabled,
        runNumber = runNumber,
        taskLimit = taskLimit,
        createCommits = createCommits
    )

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }
        expectSuccess = true
    }

    try {
        val fileTools = ProjectFileTools(projectRoot)
        val gatewayClient = ExecutionGatewayClient(
            httpClient = httpClient,
            baseUrl = config.gatewayBaseUrl,
            model = config.gatewayModel,
            gatewayApiKey = config.gatewayApiKey
        )

        val securityReviewer = if (config.securityReviewEnabled) {
            ExecutionSecurityReviewer(
                gatewayClient = gatewayClient,
                fileTools = fileTools
            )
        } else {
            null
        }

        val runner = ExecutionLoopRunner(
            config = config,
            queueLoader = ExecutionTaskQueueLoader(),
            taskExecutor = ExecutionTaskExecutor(
                gatewayClient = gatewayClient,
                fileTools = fileTools,
                fileChangeApplier = ExecutionFileChangeApplier(fileTools)
            ),
            taskValidator = ExecutionTaskValidator(
                fileTools = fileTools,
                commandRunner = ReleaseCommandRunner(projectRoot)
            ),
            securityReviewer = securityReviewer,
            gitCommitter = ExecutionGitCommitter(projectRoot),
            logWriterFactory = { runId ->
                ExecutionLogWriter(config.logDirectory, runId)
            },
            metricsWriter = ExecutionMetricsWriter(config.metricsDirectory)
        )

        System.out.println("AI Advent Challenge — Day 14")
        System.out.println("Execution Loop + Security Step")
        System.out.println("Project: $projectRoot")
        System.out.println("Queue: $queueFile")
        System.out.println("Gateway: $gatewayBaseUrl")
        System.out.println("Model: $gatewayModel")
        System.out.println("Run number: $runNumber")
        System.out.println("Max attempts per task: ${config.maxAttemptsPerTask}")
        System.out.println("Security review: ${if (securityReviewEnabled) "enabled" else "disabled"}")
        System.out.println("Git commits: ${if (createCommits) "enabled" else "disabled"}")
        taskLimit?.let { System.out.println("Task limit: $it") }
        System.out.println()

        val summary = runner.run()

        System.out.println("Execution loop finished.")
        System.out.println("Run id: ${summary.runId}")
        System.out.println("Completed tasks: ${summary.completedTasks}/${summary.totalTasks}")
        System.out.println("Consecutive without intervention: ${summary.consecutiveTasksWithoutIntervention}")
        System.out.println("Uninterrupted duration: ${"%.2f".format(summary.uninterruptedDurationMinutes)} minutes")
        System.out.println("First-pass success rate: ${"%.1f".format(summary.firstPassSuccessRate * 100)}%")
        System.out.println("Failed tasks: ${summary.failedTasks}")
        if (summary.stoppedEarly) {
            System.out.println("Stopped early: ${summary.stopReason}")
        }
        System.out.println("Log: ${config.logDirectory.resolve("${summary.runId}.md")}")
        System.out.println("Metrics: ${config.metricsDirectory.resolve("run-${summary.runNumber}-metrics.md")}")
    } finally {
        httpClient.close()
    }
}

private fun resolveQueueFile(
    projectRoot: Path,
    args: Array<String>
): Path {
    val explicit = args.firstOrNull { it.startsWith("--queue=") }
        ?.substringAfter("--queue=")
        ?.trim()

    return if (explicit.isNullOrBlank()) {
        projectRoot.resolve("execution-loop/task-pool.md")
    } else {
        projectRoot.resolve(explicit).normalize()
    }
}

private fun readIntArg(args: Array<String>, prefix: String): Int? =
    args.firstOrNull { it.startsWith(prefix) }
        ?.substringAfter(prefix)
        ?.toIntOrNull()

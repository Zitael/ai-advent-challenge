package ru.maleks.ai_advent_challenge_app.executionloop

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.Files
import java.nio.file.Path

suspend fun main(args: Array<String>) {
    val dotenv = dotenv { ignoreIfMissing = true }

    val projectRoot = Path.of(
        dotenv["PROJECT_ROOT"]
            ?: System.getenv("PROJECT_ROOT")
            ?: "."
    ).toAbsolutePath().normalize()

    val ollamaBaseUrl = dotenv["OLLAMA_BASE_URL"]
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434"

    val ollamaModel = dotenv["OLLAMA_MODEL"]
        ?: System.getenv("OLLAMA_MODEL")
        ?: "qwen3:8b"

    val queueFile = resolveQueueFile(projectRoot, args)
    val taskLimit = args.firstOrNull { it.startsWith("--limit=") }
        ?.substringAfter("--limit=")
        ?.toIntOrNull()

    val logDirectory = projectRoot.resolve("build/execution-loop/logs")
    Files.createDirectories(logDirectory)

    val config = ExecutionLoopConfig(
        projectRoot = projectRoot,
        queueFile = queueFile,
        logDirectory = logDirectory,
        maxAttemptsPerTask = 2,
        ollamaBaseUrl = ollamaBaseUrl.removeSuffix("/"),
        ollamaModel = ollamaModel,
        taskLimit = taskLimit
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
        val ollamaClient = OllamaClient(
            httpClient = httpClient,
            baseUrl = config.ollamaBaseUrl,
            model = config.ollamaModel
        )

        val runner = ExecutionLoopRunner(
            config = config,
            queueLoader = ExecutionTaskQueueLoader(),
            taskExecutor = ExecutionTaskExecutor(
                ollamaClient = ollamaClient,
                fileTools = fileTools,
                fileChangeApplier = ExecutionFileChangeApplier(fileTools)
            ),
            taskValidator = ExecutionTaskValidator(fileTools),
            logWriterFactory = { runId ->
                ExecutionLogWriter(config.logDirectory, runId)
            }
        )

        System.out.println("AI Advent Challenge — Day 5")
        System.out.println("Execution Loop")
        System.out.println("Project: $projectRoot")
        System.out.println("Queue: $queueFile")
        System.out.println("Model: $ollamaModel")
        System.out.println("Max attempts per task: ${config.maxAttemptsPerTask}")
        taskLimit?.let { System.out.println("Task limit: $it") }
        System.out.println()

        val summary = runner.run()

        System.out.println("Execution loop finished.")
        System.out.println("Run id: ${summary.runId}")
        System.out.println("Completed tasks: ${summary.completedTasks}/${summary.totalTasks}")
        System.out.println("Failed tasks: ${summary.failedTasks}")
        if (summary.stoppedEarly) {
            System.out.println("Stopped early: ${summary.stopReason}")
        }
        System.out.println("Log: ${config.logDirectory.resolve("${summary.runId}.md")}")
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

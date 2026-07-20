package ru.maleks.ai_advent_challenge_app.release

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.developer.GitDiffProvider
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.Path

suspend fun main(args: Array<String>) {
    val dotenv = dotenv { ignoreIfMissing = true }
    val projectRoot = Path.of(
        dotenv["PROJECT_ROOT"] ?: System.getenv("PROJECT_ROOT") ?: "."
    ).toAbsolutePath().normalize()

    val version = args.firstOrNull { !it.startsWith("--") }
        ?: dotenv["RELEASE_VERSION"]
        ?: System.getenv("RELEASE_VERSION")
        ?: "day35-${System.currentTimeMillis()}"

    val runChecks = args.none { it == "--skip-checks" }
    val outputDirectory = projectRoot.resolve("build/release/$version")

    val ollamaBaseUrl = dotenv["OLLAMA_BASE_URL"]
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434"
    val ollamaModel = dotenv["OLLAMA_MODEL"]
        ?: System.getenv("OLLAMA_MODEL")
        ?: "qwen3:8b"

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
        val assistant = ReleaseAssistant(
            projectRoot = projectRoot,
            ollamaClient = OllamaClient(
                httpClient = httpClient,
                baseUrl = ollamaBaseUrl.removeSuffix("/"),
                model = ollamaModel
            ),
            gitDiffProvider = GitDiffProvider(projectRoot),
            fileTools = ProjectFileTools(projectRoot),
            commandRunner = ReleaseCommandRunner(projectRoot)
        )

        println("AI Advent Challenge — Day 35")
        println("Release Assistant")
        println("Project: $projectRoot")
        println("Version: $version")
        println("Automated checks: ${if (runChecks) "enabled" else "skipped"}")
        println()
        println("1/5 Collecting Git changes...")
        println("2/5 Checking project invariants...")
        if (runChecks) println("3/5 Running Gradle test and build checks...")
        else println("3/5 Automated checks skipped...")
        println("4/5 Asking AI to prepare release notes and final review...")
        println("5/5 Writing release artifacts...")

        val result = assistant.prepare(
            ReleaseConfig(
                version = version,
                runChecks = runChecks,
                outputDirectory = outputDirectory
            )
        )

        println()
        println("Release pipeline completed.")
        println("Status: ${if (result.ready) "READY" else "BLOCKED"}")
        println("Changed files: ${result.changedFiles.size}")
        println("Blockers: ${result.blockers.size}")
        println("Warnings: ${result.warnings.size}")
        println("Artifacts: ${result.artifacts.outputDirectory}")
        println("Main report: ${result.artifacts.report}")
    } finally {
        httpClient.close()
    }
}

package ru.maleks.ai_advent_challenge_app.indirectinjection

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val model = dotenv["INDIRECT_INJECTION_MODEL"]
        ?: System.getenv("INDIRECT_INJECTION_MODEL")
        ?: "openai/gpt-4o-mini"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val outputDirectory = projectRoot.resolve("indirect-injection")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val agents = IndirectInjectionAgents(
            llmClient = OpenRouterClient(
                httpClient = httpClient,
                apiKey = apiKey,
                model = model
            )
        )

        System.out.println("AI Advent Challenge — Day 12")
        System.out.println("Indirect Prompt Injection Audit")
        System.out.println("Model: $model")
        System.out.println()

        val report = IndirectInjectionAuditRunner(agents).run(model)
        IndirectInjectionReportWriter().write(report, outputDirectory)

        writePayloadFiles(outputDirectory)

        System.out.println("Vulnerable successes: ${report.vulnerableSuccessCount}/${report.totalVectors}")
        System.out.println("Hardened successes: ${report.hardenedSuccessCount}/${report.totalVectors}")
        System.out.println("Hardened blocked: ${report.hardenedBlockedCount}")
        System.out.println()
        System.out.println("Report: ${outputDirectory.resolve("attack-results.md")}")
    } finally {
        httpClient.close()
    }
}

private fun writePayloadFiles(outputDirectory: Path) {
    val payloadsDir = outputDirectory.resolve("payloads")
    java.nio.file.Files.createDirectories(payloadsDir)

    java.nio.file.Files.writeString(
        payloadsDir.resolve("email-customer-update.txt"),
        IndirectAttackPayloads.EMAIL_RAW
    )
    java.nio.file.Files.writeString(
        payloadsDir.resolve("document-billing-report.md"),
        IndirectAttackPayloads.DOCUMENT_RAW
    )
    java.nio.file.Files.writeString(
        payloadsDir.resolve("webpage-product-status.html"),
        IndirectAttackPayloads.WEBPAGE_RAW
    )
    java.nio.file.Files.writeString(
        payloadsDir.resolve("copilot-readme-trap.md"),
        IndirectAttackPayloads.COPILOT_REPO_README_RAW
    )
}

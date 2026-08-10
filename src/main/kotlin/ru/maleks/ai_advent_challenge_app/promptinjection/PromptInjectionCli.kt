package ru.maleks.ai_advent_challenge_app.promptinjection

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

    val model = dotenv["PROMPT_INJECTION_MODEL"]
        ?: System.getenv("PROMPT_INJECTION_MODEL")
        ?: "openai/gpt-4o-mini"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val outputDirectory = projectRoot.resolve("prompt-injection")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val runner = PromptInjectionAuditRunner(
            llmClient = OpenRouterClient(
                httpClient = httpClient,
                apiKey = apiKey,
                model = model
            )
        )

        System.out.println("AI Advent Challenge — Day 11")
        System.out.println("Prompt Injection Audit")
        System.out.println("Agent: SupportAssistant")
        System.out.println("Model: $model")
        System.out.println()

        val report = runner.run(model)
        PromptInjectionReportWriter().write(report, outputDirectory)

        System.out.println("Vulnerable bypasses: ${report.vulnerableBypassCount}/3")
        System.out.println("Hardened bypasses: ${report.hardenedBypassCount}/3")
        System.out.println("Hardened blocked by guard: ${report.hardenedGuardBlockCount}/3")
        System.out.println()
        System.out.println("Written:")
        System.out.println("- ${outputDirectory.resolve("attack-results.json")}")
        System.out.println("- ${outputDirectory.resolve("attack-results.md")}")
        System.out.println("- ${outputDirectory.resolve("injection-collection.md")}")
    } finally {
        httpClient.close()
    }
}

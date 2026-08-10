package ru.maleks.ai_advent_challenge_app.promptinjection

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class PromptInjectionReportWriter {
    private val mapper = jacksonObjectMapper()

    fun write(report: PromptInjectionAuditReport, outputDirectory: Path) {
        Files.createDirectories(outputDirectory)

        val jsonPath = outputDirectory.resolve("attack-results.json")
        val markdownPath = outputDirectory.resolve("attack-results.md")
        val collectionPath = outputDirectory.resolve("injection-collection.md")

        Files.writeString(
            jsonPath,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardCharsets.UTF_8
        )

        Files.writeString(
            markdownPath,
            renderMarkdown(report),
            StandardCharsets.UTF_8
        )

        if (!collectionPath.isRegularFile()) {
            Files.writeString(
                collectionPath,
                renderCollection(),
                StandardCharsets.UTF_8
            )
        }
    }

    private fun renderMarkdown(report: PromptInjectionAuditReport): String {
        val attemptsSection = report.attempts.joinToString("\n\n") { attempt ->
            buildString {
                appendLine("### ${attempt.attackType.name} / ${attempt.securityMode.name}")
                appendLine()
                appendLine("- Outcome: **${attempt.outcome}**")
                appendLine("- Indicators: ${attempt.indicators.joinToString(", ").ifBlank { "none" }}")
                appendLine("- Latency: ${attempt.latencyMs} ms")
                appendLine()
                appendLine("**Payload**")
                appendLine("```")
                appendLine(attempt.payload.trim())
                appendLine("```")
                appendLine()
                appendLine("**Response preview**")
                appendLine("```")
                appendLine(attempt.response.trim().take(600))
                appendLine("```")
            }.trimEnd()
        }

        return buildString {
            appendLine("# Day 11 — Prompt Injection Audit")
            appendLine()
            appendLine("Agent: **${report.agent}**")
            appendLine("Model: `${report.model}`")
            appendLine("Date: ${java.time.LocalDate.now()}")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("| Metric | Value |")
            appendLine("|---|---|")
            appendLine("| Vulnerable bypasses | ${report.vulnerableBypassCount}/3 |")
            appendLine("| Hardened bypasses | ${report.hardenedBypassCount}/3 |")
            appendLine("| Blocked by input guard (hardened) | ${report.hardenedGuardBlockCount}/3 |")
            appendLine()
            appendLine("## Attack attempts")
            appendLine()
            appendLine(attemptsSection)
            appendLine()
            appendLine("## Conclusion")
            appendLine()
            appendLine("- **Vulnerable prompt** (single user blob) is susceptible to override and extraction.")
            appendLine("- **Hardened prompt** uses system role, untrusted delimiters, input guard, output guard.")
            appendLine("- For presentation: compare vulnerable SUCCEEDED vs hardened RESISTED/BLOCKED.")
        }
    }

    private fun renderCollection(): String {
        val rows = InjectionExampleCatalog.examples.joinToString("\n\n") { example ->
            """
            ## ${example.id} — ${example.title}

            - **Category:** ${example.category}
            - **Source:** ${example.source}

            **Payload**
            ```
            ${example.payload.trim()}
            ```

            - **What it does:** ${example.whatItDoes}
            - **Why it works:** ${example.whyItWorks}
            - **Mitigation:** ${example.mitigation}
            """.trimIndent()
        }

        return """
        # Prompt Injection Collection (5 examples)

        $rows
        """.trimIndent()
    }
}

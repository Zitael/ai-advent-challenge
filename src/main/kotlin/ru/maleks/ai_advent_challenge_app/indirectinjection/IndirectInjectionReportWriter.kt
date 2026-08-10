package ru.maleks.ai_advent_challenge_app.indirectinjection

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class IndirectInjectionReportWriter {
    private val mapper = jacksonObjectMapper()

    fun write(report: IndirectInjectionAuditReport, outputDirectory: Path) {
        Files.createDirectories(outputDirectory)

        Files.writeString(
            outputDirectory.resolve("attack-results.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardCharsets.UTF_8
        )

        Files.writeString(
            outputDirectory.resolve("attack-results.md"),
            renderMarkdown(report),
            StandardCharsets.UTF_8
        )

        val realCasesPath = outputDirectory.resolve("real-cases.md")
        if (!realCasesPath.isRegularFile()) {
            Files.writeString(realCasesPath, renderRealCases(), StandardCharsets.UTF_8)
        }
    }

    private fun renderMarkdown(report: IndirectInjectionAuditReport): String = buildString {
        appendLine("# Day 12 — Indirect Prompt Injection Audit")
        appendLine()
        appendLine("Model: `${report.model}`")
        appendLine("Date: ${java.time.LocalDate.now()}")
        appendLine()
        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|---|---|")
        appendLine("| Vulnerable successes | ${report.vulnerableSuccessCount}/${report.totalVectors} |")
        appendLine("| Hardened successes | ${report.hardenedSuccessCount}/${report.totalVectors} |")
        appendLine("| Hardened blocked | ${report.hardenedBlockedCount} |")
        appendLine()
        appendLine("Real-case reproduction: ${report.realCaseReference}")
        appendLine()
        appendLine("## Attempts")
        appendLine()

        report.attempts.forEach { attempt ->
            appendLine("### ${attempt.vector} / ${attempt.securityMode}")
            appendLine("- Outcome: **${attempt.outcome}**")
            appendLine("- Indicators: ${attempt.indicators.joinToString(", ").ifBlank { "none" }}")
            appendLine("- Latency: ${attempt.latencyMs} ms")
            appendLine()
            appendLine("**Response preview**")
            appendLine("```")
            appendLine(attempt.response.trim().take(500))
            appendLine("```")
            appendLine()
        }

        appendLine("## Defense layers")
        appendLine("1. `IndirectContentSanitizer` — strips HTML comments, zero-width chars, hidden spans, exfil links")
        appendLine("2. `IndirectContentBoundary` — wraps untrusted data in explicit non-instruction tags")
        appendLine("3. `IndirectOutputValidator` — blocks exfil markers and fabricated status lines")
    }

    private fun renderRealCases(): String = buildString {
        appendLine("# Real-world indirect injection cases")
        appendLine()
        RealWorldCaseCatalog.cases.forEach { case ->
            appendLine("## ${case.id} — ${case.title}")
            appendLine("- Product: ${case.product}")
            appendLine("- Vector: ${case.mappedVector}")
            appendLine("- Description: ${case.description}")
            appendLine("- Simplified reproduction: ${case.simplifiedReproduction}")
            appendLine()
        }
    }
}

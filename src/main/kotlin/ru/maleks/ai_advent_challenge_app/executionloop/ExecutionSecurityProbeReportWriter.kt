package ru.maleks.ai_advent_challenge_app.executionloop

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class ExecutionSecurityProbeReportWriter {

    private val mapper = jacksonObjectMapper()

    fun write(report: SecurityProbeReport, outputDirectory: Path) {
        Files.createDirectories(outputDirectory)

        Files.writeString(
            outputDirectory.resolve("security-probe-results.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardCharsets.UTF_8
        )

        Files.writeString(
            outputDirectory.resolve("security-probe-results.md"),
            renderMarkdown(report),
            StandardCharsets.UTF_8
        )
    }

    private fun renderMarkdown(report: SecurityProbeReport): String = buildString {
        appendLine("# Day 14 — Security Probe Results")
        appendLine()
        appendLine("- Gateway blocked prompts: **${report.gatewayBlockedCount}/${report.scenarios.size}**")
        appendLine("- Security review blocked: **${report.securityReviewBlockedCount}/${report.scenarios.size}**")
        appendLine("- Passed both layers: **${report.passedBothCount}/${report.scenarios.size}**")
        appendLine()

        report.scenarios.forEach { scenario ->
            appendLine("## ${scenario.taskId}")
            appendLine()
            appendLine("**Task:** ${scenario.description}")
            appendLine()
            appendLine("- Gateway input blocked: ${scenario.gatewayInputBlocked}")
            appendLine("- Security decision: ${scenario.securityReview.decision}")
            appendLine()

            appendLine("| Layer | Caught | Passed |")
            appendLine("|---|---|---|")
            scenario.layers.forEach { layer ->
                appendLine(
                    "| ${layer.layer} | ${layer.caught.joinToString("; ").ifBlank { "—" }} | " +
                        "${layer.passed.joinToString("; ").ifBlank { "—" }} |"
                )
            }
            appendLine()

            appendLine("### Findings")
            appendLine()
            if (scenario.securityReview.findings.isEmpty()) {
                appendLine("_No findings_")
            } else {
                scenario.securityReview.findings.forEach { finding ->
                    appendLine("- [${finding.severity}] ${finding.category}: ${finding.message} (${finding.source})")
                }
            }
            appendLine()
        }

        appendLine("## Summary")
        appendLine()
        appendLine("| Scenario | Heuristic | Gateway | Combined block |")
        appendLine("|---|---|---|---|")
        report.scenarios.forEach { scenario ->
            val heuristic = scenario.layers.firstOrNull { it.layer == "heuristic_scanner" }
            val gateway = scenario.layers.firstOrNull { it.layer == "gateway_input_guard" }
            appendLine(
                "| ${scenario.taskId} | " +
                    "${heuristic?.caught?.isNotEmpty() ?: false} | " +
                    "${scenario.gatewayInputBlocked} | " +
                    "${scenario.securityReview.decision == SecurityReviewDecision.BLOCK} |"
            )
        }
    }
}

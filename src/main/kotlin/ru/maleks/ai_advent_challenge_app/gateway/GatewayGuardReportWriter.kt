package ru.maleks.ai_advent_challenge_app.gateway

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class GatewayGuardReportWriter {
    private val mapper = jacksonObjectMapper()

    fun write(report: GuardTestReport, outputDirectory: Path) {
        java.nio.file.Files.createDirectories(outputDirectory)

        java.nio.file.Files.writeString(
            outputDirectory.resolve("guard-test-results.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardCharsets.UTF_8
        )

        java.nio.file.Files.writeString(
            outputDirectory.resolve("guard-test-results.md"),
            renderMarkdown(report),
            StandardCharsets.UTF_8
        )
    }

    private fun renderMarkdown(report: GuardTestReport): String = buildString {
        appendLine("# Day 13 — Gateway Input Guard Test Results")
        appendLine()
        appendLine("Passed: **${report.passedCases}/${report.totalCases}**")
        appendLine()
        appendLine("| ID | Description | Expected | Actual | Findings | Pass |")
        appendLine("|---|---|---|---|---|---|")
        report.results.forEach { result ->
            appendLine(
                "| ${result.testCase.id} | ${result.testCase.description} | " +
                    "${result.testCase.expectedAction} | ${result.actualAction} | " +
                    "${result.actualFindingTypes.joinToString(";")} | ${if (result.passed) "PASS" else "FAIL"} |"
            )
        }
    }
}

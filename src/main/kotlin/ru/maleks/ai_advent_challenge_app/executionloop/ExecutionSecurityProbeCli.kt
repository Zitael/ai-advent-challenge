package ru.maleks.ai_advent_challenge_app.executionloop

import io.github.cdimascio.dotenv.dotenv
import java.nio.file.Path

fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val projectRoot = Path.of(
        dotenv["PROJECT_ROOT"]
            ?: System.getenv("PROJECT_ROOT")
            ?: "."
    ).toAbsolutePath().normalize()

    val outputDirectory = projectRoot.resolve("execution-loop/security-probe")
    val runner = ExecutionSecurityProbeRunner()
    val report = runner.runAll(projectRoot)

    ExecutionSecurityProbeReportWriter().write(report, outputDirectory)

    System.out.println("AI Advent Challenge — Day 14")
    System.out.println("Security Probe Runner")
    System.out.println("Scenarios: ${report.scenarios.size}")
    System.out.println("Gateway blocked: ${report.gatewayBlockedCount}")
    System.out.println("Security review blocked: ${report.securityReviewBlockedCount}")
    System.out.println("Passed both: ${report.passedBothCount}")
    System.out.println()

    report.scenarios.forEach { scenario ->
        System.out.println(
            "- ${scenario.taskId}: gatewayBlocked=${scenario.gatewayInputBlocked}, " +
                "decision=${scenario.securityReview.decision}, " +
                "findings=${scenario.securityReview.findings.size}"
        )
    }

    System.out.println()
    System.out.println("Report: ${outputDirectory.resolve("security-probe-results.md")}")
}

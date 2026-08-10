package ru.maleks.ai_advent_challenge_app.gateway

fun main() {
    val report = GatewayGuardTestRunner().runAll()
    val outputDirectory = java.nio.file.Path.of("llm-gateway")

    GatewayGuardReportWriter().write(report, outputDirectory)

    System.out.println("AI Advent Challenge — Day 13")
    System.out.println("Gateway Input Guard Tests")
    System.out.println("Passed: ${report.passedCases}/${report.totalCases}")
    System.out.println("Failed: ${report.failedCases}/${report.totalCases}")
    System.out.println()

    report.results.forEach { result ->
        val status = if (result.passed) "PASS" else "FAIL"
        System.out.println(
            "- ${result.testCase.id}: $status " +
                "(${result.actualAction}, findings=${result.actualFindingTypes})"
        )
    }

    System.out.println()
    System.out.println("Report: ${outputDirectory.resolve("guard-test-results.md")}")
}

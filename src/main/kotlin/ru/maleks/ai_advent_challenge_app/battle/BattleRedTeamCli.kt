package ru.maleks.ai_advent_challenge_app.battle

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

fun main() {
    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val outputDirectory = projectRoot.resolve("battle-pipeline")
    Files.createDirectories(outputDirectory)

    val report = BattleRedTeamRunner().runAll()
    val markdown = renderMarkdown(report)

    Files.writeString(
        outputDirectory.resolve("red-team-baseline.md"),
        markdown,
        StandardCharsets.UTF_8
    )

    System.out.println("AI Advent Challenge — Battle Red Team Baseline")
    System.out.println("Blocked: ${report.blockedCount}/${report.totalAttacks}")
    System.out.println()

    report.results.forEach { result ->
        val status = if (result.blocked || result.attack.expectedBlocker == "none") "PASS" else "FAIL"
        System.out.println(
            "- ${result.attack.id}: $status blocked=${result.blocked} by=${result.blockedBy}"
        )
    }

    System.out.println()
    System.out.println("Report: ${outputDirectory.resolve("red-team-baseline.md")}")
}

private fun renderMarkdown(report: BattleRedTeamReport): String = buildString {
    appendLine("# Red Team Baseline — Battle Pipeline")
    appendLine()
    appendLine("Blocked: **${report.blockedCount}/${report.totalAttacks}**")
    appendLine()
    appendLine("| ID | Category | Expected | Blocked | Blocked by |")
    appendLine("|---|---|---|---|---|")
    report.results.forEach { result ->
        appendLine(
            "| ${result.attack.id} | ${result.attack.category} | ${result.attack.expectedBlocker} | " +
                "${result.blocked} | ${result.blockedBy.joinToString(", ").ifBlank { "—" }} |"
        )
    }
    appendLine()
    appendLine("## Payloads")
    appendLine()
    report.results.forEach { result ->
        appendLine("### ${result.attack.id}")
        appendLine()
        appendLine("```text")
        appendLine(result.attack.payload)
        appendLine("```")
        appendLine()
    }
}

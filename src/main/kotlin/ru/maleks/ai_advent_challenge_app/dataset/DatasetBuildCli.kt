package ru.maleks.ai_advent_challenge_app.dataset

import java.nio.file.Path

fun main() {
    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val datasetDirectory = projectRoot.resolve("dataset")

    val builder = DatasetBuilder(
        realTicketSource = RealTicketSource(
            realTicketsFile = datasetDirectory.resolve("raw/real-tickets.json")
        )
    )

    val report = builder.build(datasetDirectory)
    val validator = DatasetValidator()

    val trainReport = validator.validate(datasetDirectory.resolve("train.jsonl"))
    val evalReport = validator.validate(datasetDirectory.resolve("eval.jsonl"))

    System.out.println("AI Advent Challenge — Day 6")
    System.out.println("Dataset Builder")
    System.out.println()
    System.out.println("Raw examples: ${report.totalRaw}")
    System.out.println("After cleaning: ${report.afterCleaning}")
    System.out.println("Real: ${report.realCount} (${percentage(report.realCount, report.afterCleaning)}%)")
    System.out.println("Synthetic: ${report.syntheticCount}")
    System.out.println("Train: ${report.trainCount} -> ${report.trainPath}")
    System.out.println("Eval: ${report.evalCount} -> ${report.evalPath}")
    System.out.println()
    System.out.println("Validation train: ${if (trainReport.passed) "PASS" else "FAIL"} (${trainReport.validLines}/${trainReport.totalLines})")
    System.out.println("Validation eval: ${if (evalReport.passed) "PASS" else "FAIL"} (${evalReport.validLines}/${evalReport.totalLines})")

    if (!trainReport.passed || !evalReport.passed) {
        (trainReport.issues + evalReport.issues).take(10).forEach { issue ->
            System.out.println("- line ${issue.lineNumber}: ${issue.message}")
        }
        kotlin.error("Dataset validation failed during build")
    }
}

private fun percentage(part: Int, total: Int): String {
    if (total == 0) {
        return "0.0"
    }
    return "%.1f".format(part * 100.0 / total)
}

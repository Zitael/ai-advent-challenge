package ru.maleks.ai_advent_challenge_app.dataset

import java.nio.file.Path

fun main(args: Array<String>) {
    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val datasetDirectory = projectRoot.resolve("dataset")
    val validator = DatasetValidator()

    val files = listOf(
        datasetDirectory.resolve("train.jsonl"),
        datasetDirectory.resolve("eval.jsonl")
    )

    System.out.println("AI Advent Challenge — Day 6")
    System.out.println("Dataset Validator")
    System.out.println()

    var allPassed = true

    files.forEach { file ->
        val report = validator.validate(file)
        System.out.println("${file.fileName}: ${if (report.passed) "PASS" else "FAIL"} (${report.validLines}/${report.totalLines})")

        if (!report.passed) {
            allPassed = false
            report.issues.take(20).forEach { issue ->
                System.out.println("  line ${issue.lineNumber}: ${issue.message}")
            }
        }
    }

    if (!allPassed) {
        kotlin.error("Dataset validation failed")
    }
}

package ru.maleks.ai_advent_challenge_app.projectfiles

import io.github.cdimascio.dotenv.dotenv
import java.nio.file.Path

fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val projectRoot = Path.of(
        dotenv["PROJECT_ROOT"] ?: System.getenv("PROJECT_ROOT") ?: "."
    ).toAbsolutePath().normalize()

    val assistant = ProjectFileAssistant(ProjectFileTools(projectRoot))
    val reader = System.`in`.bufferedReader()

    println("AI Advent Challenge — Day 34")
    println("Project File Assistant")
    println("Project: $projectRoot")
    println("Give a goal, not a file-level command.")
    printExamples()

    while (true) {
        print("\nYou: ")
        System.out.flush()
        val input = reader.readLine()?.trim() ?: break
        if (input.equals("/exit", true)) break
        if (input.equals("/help", true)) {
            printExamples()
            continue
        }
        if (input.isBlank()) continue

        println("\nAssistant is inspecting project files...")
        val result = runCatching { assistant.execute(input) }
            .getOrElse { exception ->
                println("Failed: ${exception.message}")
                continue
            }
        println("Inspected files: ${result.inspectedFiles.distinct().size}")
        println("Saved changes: ${result.changes.size}")
        println("\n${result.summary}")
    }
}

private fun printExamples() {
    println("Examples:")
    println("  find OllamaClient")
    println("  найди все использования SupportAssistant")
    println("  обнови документацию проекта")
    println("  создай changelog")
    println("  проверь инварианты проекта")
    println("  /exit")
}

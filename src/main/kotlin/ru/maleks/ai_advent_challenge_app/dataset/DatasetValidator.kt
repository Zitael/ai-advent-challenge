package ru.maleks.ai_advent_challenge_app.dataset

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class DatasetValidator {
    private val mapper = jacksonObjectMapper()

    fun validate(path: Path): ValidationReport {
        require(path.isRegularFile()) {
            "Dataset file not found: $path"
        }

        val lines = Files.readAllLines(path, StandardCharsets.UTF_8)
        val issues = mutableListOf<ValidationIssue>()
        var validLines = 0

        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()

            if (line.isBlank()) {
                issues += ValidationIssue(lineNumber, "Empty line")
                return@forEachIndexed
            }

            val parsed = runCatching {
                mapper.readValue<Map<String, Any>>(line)
            }.getOrElse { exception ->
                issues += ValidationIssue(lineNumber, "Invalid JSON: ${exception.message}")
                return@forEachIndexed
            }

            val messagesRaw = parsed["messages"]
            if (messagesRaw !is List<*>) {
                issues += ValidationIssue(lineNumber, "Missing or invalid messages array")
                return@forEachIndexed
            }

            if (messagesRaw.size != 3) {
                issues += ValidationIssue(lineNumber, "Expected exactly 3 messages, got ${messagesRaw.size}")
                return@forEachIndexed
            }

            val roles = messagesRaw.mapNotNull { item ->
                (item as? Map<*, *>)?.get("role")?.toString()?.lowercase()
            }

            if (roles != listOf("system", "user", "assistant")) {
                issues += ValidationIssue(lineNumber, "Roles must be system, user, assistant in order")
                return@forEachIndexed
            }

            messagesRaw.forEachIndexed { messageIndex, item ->
                val content = (item as? Map<*, *>)?.get("content")?.toString()?.trim().orEmpty()
                if (content.isBlank()) {
                    issues += ValidationIssue(
                        lineNumber,
                        "Empty content in message ${messageIndex + 1}"
                    )
                }
            }

            if (issues.none { it.lineNumber == lineNumber }) {
                validLines++
            }
        }

        return ValidationReport(
            filePath = path.toString(),
            totalLines = lines.count { it.isNotBlank() },
            validLines = validLines,
            issues = issues
        )
    }
}

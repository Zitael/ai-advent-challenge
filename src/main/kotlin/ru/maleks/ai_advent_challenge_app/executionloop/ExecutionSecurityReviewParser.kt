package ru.maleks.ai_advent_challenge_app.executionloop

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class ExecutionSecurityReviewParser {

    private val mapper = jacksonObjectMapper()

    fun parse(rawAnswer: String): ParsedSecurityReview {
        val jsonPayload = extractJson(rawAnswer)
            ?: return ParsedSecurityReview(
                findings = emptyList(),
                summary = "Unable to parse security review response",
                parseError = true
            )

        return runCatching {
            val root: JsonNode = mapper.readTree(jsonPayload)
            val findings = root.path("findings").mapNotNull { node ->
                val severity = SecuritySeverity.fromRaw(node.path("severity").asText(null))
                    ?: return@mapNotNull null

                SecurityFinding(
                    severity = severity,
                    category = node.path("category").asText("other"),
                    file = node.path("file").asText(null),
                    line = node.path("line").takeIf { it.isInt }?.asInt(),
                    message = node.path("message").asText("Security issue detected"),
                    source = SecurityFindingSource.LLM_REVIEW
                )
            }

            ParsedSecurityReview(
                findings = findings,
                summary = root.path("summary").asText("Security review completed"),
                parseError = false
            )
        }.getOrElse {
            ParsedSecurityReview(
                findings = emptyList(),
                summary = "Security review JSON parse error: ${it.message}",
                parseError = true
            )
        }
    }

    private fun extractJson(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            return trimmed
        }

        val blockMatch = JSON_BLOCK.find(trimmed)
        if (blockMatch != null) {
            return blockMatch.groupValues[1].trim()
        }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1)
        }

        return null
    }

    companion object {
        private val JSON_BLOCK = Regex("""```(?:json)?\s*([\s\S]*?)```""")
    }
}

data class ParsedSecurityReview(
    val findings: List<SecurityFinding>,
    val summary: String,
    val parseError: Boolean
)

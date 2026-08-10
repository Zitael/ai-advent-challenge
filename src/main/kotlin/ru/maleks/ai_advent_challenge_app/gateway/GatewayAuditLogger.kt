package ru.maleks.ai_advent_challenge_app.gateway

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GatewayAuditLogger(
    private val logPath: Path
) {
    private val mapper = jacksonObjectMapper()
    private val timestampFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun log(entry: GatewayAuditEntry) {
        logPath.parent?.let { Files.createDirectories(it) }

        val line = mapper.writeValueAsString(entry) + System.lineSeparator()
        Files.writeString(
            logPath,
            line,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }

    fun interceptedSecrets(entries: List<GatewayAuditEntry>): List<String> =
        entries.flatMap { it.inputFindings }.distinct()

    companion object {
        fun createEntry(
            clientIp: String,
            model: String,
            input: InputGuardResult,
            output: OutputGuardResult?,
            usage: GatewayUsage?,
            costUsd: Double?,
            requestPreview: String,
            responsePreview: String,
            blocked: Boolean
        ): GatewayAuditEntry =
            GatewayAuditEntry(
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                clientIp = clientIp,
                model = model,
                inputGuardAction = input.action,
                inputFindings = input.findings.map { it.type.label },
                outputViolations = output?.violations.orEmpty(),
                blocked = blocked,
                promptTokens = usage?.promptTokens,
                completionTokens = usage?.completionTokens,
                costUsd = costUsd,
                requestPreview = requestPreview.take(300),
                responsePreview = responsePreview.take(300)
            )
    }
}

class GatewayCostTracker {
    private var totalRequests: Int = 0
    private var totalPromptTokens: Int = 0
    private var totalCompletionTokens: Int = 0
    private var totalCostUsd: Double = 0.0

    fun record(usage: GatewayUsage?, costUsd: Double?) {
        totalRequests++
        totalPromptTokens += usage?.promptTokens ?: 0
        totalCompletionTokens += usage?.completionTokens ?: 0
        totalCostUsd += costUsd ?: 0.0
    }

    fun snapshot(): CostSnapshot =
        CostSnapshot(
            totalRequests = totalRequests,
            totalPromptTokens = totalPromptTokens,
            totalCompletionTokens = totalCompletionTokens,
            totalCostUsd = totalCostUsd
        )

    data class CostSnapshot(
        val totalRequests: Int,
        val totalPromptTokens: Int,
        val totalCompletionTokens: Int,
        val totalCostUsd: Double
    )
}

package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.gateway.InputGuardAction

enum class SecuritySeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromRaw(value: String?): SecuritySeverity? = when (value?.trim()?.uppercase()) {
            "CRITICAL" -> CRITICAL
            "HIGH" -> HIGH
            "MEDIUM" -> MEDIUM
            "LOW" -> LOW
            else -> null
        }
    }
}

enum class SecurityReviewDecision {
    PASS,
    PASS_WITH_WARNINGS,
    BLOCK
}

enum class ExecutionLlmPurpose {
    CODE_GENERATION,
    SECURITY_REVIEW
}

data class SecurityFinding(
    val severity: SecuritySeverity,
    val category: String,
    val file: String?,
    val line: Int?,
    val message: String,
    val source: SecurityFindingSource
)

enum class SecurityFindingSource {
    HEURISTIC,
    LLM_REVIEW,
    GATEWAY_INPUT,
    GATEWAY_OUTPUT
}

data class GatewayCallLog(
    val purpose: ExecutionLlmPurpose,
    val inputGuardAction: InputGuardAction,
    val inputFindings: List<String>,
    val outputViolations: List<String>,
    val blocked: Boolean,
    val answerPreview: String
)

data class ExecutionSecurityReviewResult(
    val decision: SecurityReviewDecision,
    val findings: List<SecurityFinding>,
    val feedback: String?,
    val gatewayCalls: List<GatewayCallLog>,
    val summary: String
) {
    val blockingFindings: List<SecurityFinding>
        get() = findings.filter { it.severity == SecuritySeverity.CRITICAL || it.severity == SecuritySeverity.HIGH }

    val warningFindings: List<SecurityFinding>
        get() = findings.filter { it.severity == SecuritySeverity.MEDIUM || it.severity == SecuritySeverity.LOW }
}

data class SecurityProbeScenario(
    val taskId: String,
    val description: String,
    val simulatedCode: Map<String, String>
)

data class SecurityProbeLayerResult(
    val layer: String,
    val caught: List<String>,
    val passed: List<String>
)

data class SecurityProbeTaskResult(
    val taskId: String,
    val description: String,
    val securityReview: ExecutionSecurityReviewResult,
    val gatewayInputBlocked: Boolean,
    val layers: List<SecurityProbeLayerResult>
)

data class SecurityProbeReport(
    val scenarios: List<SecurityProbeTaskResult>,
    val gatewayBlockedCount: Int,
    val securityReviewBlockedCount: Int,
    val passedBothCount: Int
)

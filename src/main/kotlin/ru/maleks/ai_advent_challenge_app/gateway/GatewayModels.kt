package ru.maleks.ai_advent_challenge_app.gateway

enum class InputGuardMode {
    BLOCK,
    MASK
}

enum class InputGuardAction {
    ALLOW,
    BLOCK,
    MASK
}

enum class SecretType(val label: String) {
    API_KEY_OPENAI("openai_api_key"),
    API_KEY_GITHUB("github_pat"),
    API_KEY_AWS("aws_access_key"),
    CREDIT_CARD("credit_card"),
    EMAIL("email"),
    PHONE("phone"),
    BASE64_SECRET("base64_secret")
}

data class SecretFinding(
    val type: SecretType,
    val matched: String,
    val redacted: String
)

data class InputGuardResult(
    val action: InputGuardAction,
    val originalPrompt: String,
    val processedPrompt: String,
    val findings: List<SecretFinding>,
    val warning: String? = null
)

data class OutputGuardResult(
    val allowed: Boolean,
    val processedOutput: String,
    val violations: List<String>,
    val warning: String? = null
)

data class GatewayChatRequest(
    val model: String? = null,
    val messages: List<GatewayMessage>,
    val inputGuardMode: InputGuardMode = InputGuardMode.BLOCK,
    val inputGuardText: String? = null,
    val temperature: Double? = null
)

data class GatewayMessage(
    val role: String,
    val content: String
)

data class GatewayChatResponse(
    val id: String,
    val answer: String,
    val inputGuardAction: InputGuardAction,
    val outputGuardAllowed: Boolean,
    val findings: List<String>,
    val violations: List<String>,
    val usage: GatewayUsage?,
    val costUsd: Double?
)

data class GatewayUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class GatewayAuditEntry(
    val timestamp: String,
    val clientIp: String,
    val model: String,
    val inputGuardAction: InputGuardAction,
    val inputFindings: List<String>,
    val outputViolations: List<String>,
    val blocked: Boolean,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val costUsd: Double?,
    val requestPreview: String,
    val responsePreview: String
)

data class GuardTestCase(
    val id: String,
    val description: String,
    val prompt: String,
    val mode: InputGuardMode,
    val expectedAction: InputGuardAction,
    val expectedFindingTypes: Set<SecretType>
)

data class GuardTestCaseResult(
    val testCase: GuardTestCase,
    val actualAction: InputGuardAction,
    val actualFindingTypes: Set<SecretType>,
    val passed: Boolean,
    val processedPreview: String
)

data class GuardTestReport(
    val totalCases: Int,
    val passedCases: Int,
    val failedCases: Int,
    val results: List<GuardTestCaseResult>
)

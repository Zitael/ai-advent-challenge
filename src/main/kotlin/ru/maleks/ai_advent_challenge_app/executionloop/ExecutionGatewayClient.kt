package ru.maleks.ai_advent_challenge_app.executionloop

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.maleks.ai_advent_challenge_app.gateway.GatewayChatRequest
import ru.maleks.ai_advent_challenge_app.gateway.GatewayChatResponse
import ru.maleks.ai_advent_challenge_app.gateway.GatewayMessage
import ru.maleks.ai_advent_challenge_app.gateway.InputGuardAction
import ru.maleks.ai_advent_challenge_app.gateway.InputGuardMode

class ExecutionGatewayClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val model: String,
    private val gatewayApiKey: String? = null,
    private val inputGuardMode: InputGuardMode = InputGuardMode.BLOCK,
    private val contextSanitizer: ExecutionContextSanitizer = ExecutionContextSanitizer()
) {

    suspend fun complete(
        prompt: String,
        purpose: ExecutionLlmPurpose,
        temperature: Double? = null
    ): ExecutionGatewayResult {
        val sanitized = contextSanitizer.sanitize(prompt)

        val request = GatewayChatRequest(
            model = model,
            messages = listOf(GatewayMessage(role = "user", content = sanitized.sanitizedText)),
            inputGuardMode = inputGuardMode,
            temperature = temperature
        )

        val response: GatewayChatResponse = httpClient.post("$baseUrl/api/gateway/chat") {
            contentType(ContentType.Application.Json)
            gatewayApiKey?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(request)
        }.body()

        val blocked = response.inputGuardAction == InputGuardAction.BLOCK ||
            !response.outputGuardAllowed

        return ExecutionGatewayResult(
            answer = response.answer,
            purpose = purpose,
            inputGuardAction = response.inputGuardAction,
            inputFindings = response.findings,
            outputViolations = response.violations,
            blocked = blocked,
            usage = response.usage,
            costUsd = response.costUsd,
            sanitizedContext = sanitized
        )
    }
}

data class ExecutionGatewayResult(
    val answer: String,
    val purpose: ExecutionLlmPurpose,
    val inputGuardAction: InputGuardAction,
    val inputFindings: List<String>,
    val outputViolations: List<String>,
    val blocked: Boolean,
    val usage: ru.maleks.ai_advent_challenge_app.gateway.GatewayUsage?,
    val costUsd: Double?,
    val sanitizedContext: ExecutionSanitizedContext
) {
    fun toGatewayCallLog(): GatewayCallLog = GatewayCallLog(
        purpose = purpose,
        inputGuardAction = inputGuardAction,
        inputFindings = inputFindings,
        outputViolations = outputViolations,
        blocked = blocked,
        answerPreview = answer.take(300)
    )
}

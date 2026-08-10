package ru.maleks.ai_advent_challenge_app.gateway

import ru.maleks.ai_advent_challenge_app.privateai.security.InMemoryRateLimiter
import ru.maleks.ai_advent_challenge_app.privateai.security.RateLimitDecision
import java.util.UUID

class LlmGatewayService(
    private val config: LlmGatewayConfig,
    private val proxyClient: OpenRouterProxyClient,
    private val inputGuard: InputGuard = InputGuard(),
    private val outputGuard: OutputGuard = OutputGuard(),
    private val auditLogger: GatewayAuditLogger,
    private val costTracker: GatewayCostTracker = GatewayCostTracker(),
    private val rateLimiter: InMemoryRateLimiter = InMemoryRateLimiter(
        maxRequests = config.rateLimitPerMinute,
        windowSeconds = 60
    )
) {

    suspend fun chat(
        clientIp: String,
        request: GatewayChatRequest
    ): GatewayServiceResult {
        val rateLimit = rateLimiter.tryAcquire(clientIp)
        if (rateLimit is RateLimitDecision.Denied) {
            return GatewayServiceResult.RateLimited(rateLimit.retryAfterSeconds)
        }

        val userPrompt = request.messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        val guardMode = request.inputGuardMode
        val inputResult = inputGuard.inspect(userPrompt, guardMode)

        if (inputResult.action == InputGuardAction.BLOCK) {
            auditLogger.log(
                GatewayAuditLogger.createEntry(
                    clientIp = clientIp,
                    model = request.model ?: config.defaultModel,
                    input = inputResult,
                    output = null,
                    usage = null,
                    costUsd = null,
                    requestPreview = userPrompt,
                    responsePreview = inputResult.warning.orEmpty(),
                    blocked = true
                )
            )

            return GatewayServiceResult.InputBlocked(inputResult)
        }

        val upstreamMessages = buildUpstreamMessages(request, inputResult.processedPrompt)
        val model = request.model ?: config.defaultModel

        val proxyResult = proxyClient.chat(
            model = model,
            messages = upstreamMessages,
            temperature = request.temperature
        )

        val outputResult = outputGuard.inspect(proxyResult.answer)
        costTracker.record(proxyResult.usage, proxyResult.costUsd)

        val blockedByOutput = !outputResult.allowed
        val finalAnswer = if (blockedByOutput) {
            outputResult.warning ?: "Response blocked by output guard"
        } else {
            outputResult.processedOutput
        }

        auditLogger.log(
            GatewayAuditLogger.createEntry(
                clientIp = clientIp,
                model = model,
                input = inputResult,
                output = outputResult,
                usage = proxyResult.usage,
                costUsd = proxyResult.costUsd,
                requestPreview = userPrompt,
                responsePreview = finalAnswer,
                blocked = blockedByOutput
            )
        )

        return GatewayServiceResult.Success(
            GatewayChatResponse(
                id = UUID.randomUUID().toString(),
                answer = finalAnswer,
                inputGuardAction = inputResult.action,
                outputGuardAllowed = outputResult.allowed,
                findings = inputResult.findings.map { it.type.label },
                violations = outputResult.violations,
                usage = proxyResult.usage,
                costUsd = proxyResult.costUsd
            )
        )
    }

    fun costSnapshot(): GatewayCostTracker.CostSnapshot = costTracker.snapshot()

    private fun buildUpstreamMessages(
        request: GatewayChatRequest,
        processedUserPrompt: String
    ): List<GatewayMessage> {
        val lastUserIndex = request.messages.indexOfLast { it.role == "user" }
        if (lastUserIndex < 0) {
            return request.messages
        }

        return request.messages.mapIndexed { index, message ->
            if (index == lastUserIndex) {
                message.copy(content = processedUserPrompt)
            } else {
                message
            }
        }
    }
}

sealed interface GatewayServiceResult {
    data class Success(val response: GatewayChatResponse) : GatewayServiceResult
    data class InputBlocked(val input: InputGuardResult) : GatewayServiceResult
    data class RateLimited(val retryAfterSeconds: Long) : GatewayServiceResult
}

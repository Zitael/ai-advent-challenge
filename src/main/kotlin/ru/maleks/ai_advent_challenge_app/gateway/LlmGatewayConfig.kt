package ru.maleks.ai_advent_challenge_app.gateway

import io.github.cdimascio.dotenv.Dotenv

data class LlmGatewayConfig(
    val host: String,
    val port: Int,
    val upstreamApiKey: String,
    val defaultModel: String,
    val gatewayApiKey: String?,
    val rateLimitPerMinute: Int,
    val auditLogPath: String,
    val defaultInputGuardMode: InputGuardMode
) {
    companion object {
        fun from(dotenv: Dotenv): LlmGatewayConfig {
            val modeRaw = dotenv["GATEWAY_INPUT_GUARD_MODE"]
                ?: System.getenv("GATEWAY_INPUT_GUARD_MODE")
                ?: "BLOCK"

            return LlmGatewayConfig(
                host = dotenv["GATEWAY_HOST"] ?: System.getenv("GATEWAY_HOST") ?: "127.0.0.1",
                port = (dotenv["GATEWAY_PORT"] ?: System.getenv("GATEWAY_PORT") ?: "8090").toInt(),
                upstreamApiKey = dotenv["OPENROUTER_API_KEY"]
                    ?: System.getenv("OPENROUTER_API_KEY")
                    ?: error("OPENROUTER_API_KEY is required"),
                defaultModel = dotenv["GATEWAY_DEFAULT_MODEL"]
                    ?: System.getenv("GATEWAY_DEFAULT_MODEL")
                    ?: "openai/gpt-4o-mini",
                gatewayApiKey = dotenv["GATEWAY_API_KEY"] ?: System.getenv("GATEWAY_API_KEY"),
                rateLimitPerMinute = (dotenv["GATEWAY_RATE_LIMIT_PER_MINUTE"]
                    ?: System.getenv("GATEWAY_RATE_LIMIT_PER_MINUTE")
                    ?: "30").toInt(),
                auditLogPath = dotenv["GATEWAY_AUDIT_LOG"]
                    ?: System.getenv("GATEWAY_AUDIT_LOG")
                    ?: "llm-gateway/logs/audit.jsonl",
                defaultInputGuardMode = InputGuardMode.valueOf(modeRaw.uppercase())
            )
        }
    }
}

package ru.maleks.ai_advent_challenge_app.battle

import io.github.cdimascio.dotenv.Dotenv

data class BattlePipelineConfig(
    val host: String,
    val port: Int,
    val apiKey: String,
    val upstreamApiKey: String,
    val defaultModel: String,
    val rateLimitPerMinute: Int,
    val auditLogPath: String,
    val maxMessageChars: Int,
    val maxHistoryMessages: Int,
    val maxConcurrentGenerations: Int,
    val workspaceDirectory: String
) {
    companion object {
        fun from(dotenv: Dotenv): BattlePipelineConfig {
            val apiKey = dotenv["BATTLE_API_KEY"]
                ?: System.getenv("BATTLE_API_KEY")
                ?: dotenv["GATEWAY_API_KEY"]
                ?: System.getenv("GATEWAY_API_KEY")
                ?: error("BATTLE_API_KEY is required for battle deployment")

            require(apiKey.length >= 20) {
                "BATTLE_API_KEY must be at least 20 characters"
            }

            return BattlePipelineConfig(
                host = dotenv["BATTLE_HOST"] ?: System.getenv("BATTLE_HOST") ?: "0.0.0.0",
                port = (dotenv["BATTLE_PORT"] ?: System.getenv("BATTLE_PORT") ?: "8090").toInt(),
                apiKey = apiKey,
                upstreamApiKey = dotenv["OPENROUTER_API_KEY"]
                    ?: System.getenv("OPENROUTER_API_KEY")
                    ?: error("OPENROUTER_API_KEY is required"),
                defaultModel = dotenv["GATEWAY_DEFAULT_MODEL"]
                    ?: System.getenv("GATEWAY_DEFAULT_MODEL")
                    ?: "openai/gpt-4o-mini",
                rateLimitPerMinute = (dotenv["GATEWAY_RATE_LIMIT_PER_MINUTE"]
                    ?: System.getenv("GATEWAY_RATE_LIMIT_PER_MINUTE")
                    ?: "30").toInt(),
                auditLogPath = dotenv["GATEWAY_AUDIT_LOG"]
                    ?: System.getenv("GATEWAY_AUDIT_LOG")
                    ?: "llm-gateway/logs/audit.jsonl",
                maxMessageChars = (dotenv["BATTLE_MAX_MESSAGE_CHARS"]
                    ?: System.getenv("BATTLE_MAX_MESSAGE_CHARS")
                    ?: "4000").toInt(),
                maxHistoryMessages = (dotenv["BATTLE_MAX_HISTORY"]
                    ?: System.getenv("BATTLE_MAX_HISTORY")
                    ?: "20").toInt(),
                maxConcurrentGenerations = (dotenv["BATTLE_MAX_CONCURRENT"]
                    ?: System.getenv("BATTLE_MAX_CONCURRENT")
                    ?: "4").toInt(),
                workspaceDirectory = dotenv["BATTLE_WORKSPACE_DIR"]
                    ?: System.getenv("BATTLE_WORKSPACE_DIR")
                    ?: "battle-pipeline/workspace"
            )
        }
    }
}

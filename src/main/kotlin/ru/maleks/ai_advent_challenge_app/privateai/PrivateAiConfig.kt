package ru.maleks.ai_advent_challenge_app.privateai

import io.github.cdimascio.dotenv.Dotenv

data class PrivateAiConfig(
    val host: String,
    val port: Int,
    val apiKey: String,
    val ollamaBaseUrl: String,
    val ollamaModel: String,
    val rateLimitRequests: Int,
    val rateLimitWindowSeconds: Long,
    val maxMessageChars: Int,
    val maxHistoryMessages: Int,
    val maxConcurrentGenerations: Int,
    val temperature: Double,
    val maxTokens: Int,
    val contextWindow: Int
) {
    companion object {

        fun from(dotenv: Dotenv): PrivateAiConfig {
            return PrivateAiConfig(
                host = dotenv.value("PRIVATE_AI_HOST", "0.0.0.0"),
                port = dotenv.intValue("PRIVATE_AI_PORT", 8080),
                apiKey = dotenv.requiredValue("PRIVATE_AI_API_KEY"),
                ollamaBaseUrl = dotenv
                    .value(
                        name = "OLLAMA_BASE_URL",
                        defaultValue = "http://localhost:11434"
                    )
                    .removeSuffix("/"),
                ollamaModel = dotenv.value(
                    name = "OLLAMA_MODEL",
                    defaultValue = "qwen3:8b"
                ),
                rateLimitRequests = dotenv.intValue(
                    name = "PRIVATE_AI_RATE_LIMIT_REQUESTS",
                    defaultValue = 5
                ),
                rateLimitWindowSeconds = dotenv.longValue(
                    name = "PRIVATE_AI_RATE_LIMIT_WINDOW_SECONDS",
                    defaultValue = 60
                ),
                maxMessageChars = dotenv.intValue(
                    name = "PRIVATE_AI_MAX_MESSAGE_CHARS",
                    defaultValue = 4_000
                ),
                maxHistoryMessages = dotenv.intValue(
                    name = "PRIVATE_AI_MAX_HISTORY_MESSAGES",
                    defaultValue = 10
                ),
                maxConcurrentGenerations = dotenv.intValue(
                    name = "PRIVATE_AI_MAX_CONCURRENT_GENERATIONS",
                    defaultValue = 1
                ),
                temperature = dotenv.doubleValue(
                    name = "PRIVATE_AI_TEMPERATURE",
                    defaultValue = 0.2
                ),
                maxTokens = dotenv.intValue(
                    name = "PRIVATE_AI_MAX_TOKENS",
                    defaultValue = 500
                ),
                contextWindow = dotenv.intValue(
                    name = "PRIVATE_AI_CONTEXT_WINDOW",
                    defaultValue = 8_192
                )
            ).also { config ->
                require(config.apiKey.length >= 20) {
                    "PRIVATE_AI_API_KEY must contain at least 20 characters"
                }

                require(config.rateLimitRequests > 0)
                require(config.rateLimitWindowSeconds > 0)
                require(config.maxMessageChars > 0)
                require(config.maxHistoryMessages >= 2)
                require(config.maxConcurrentGenerations > 0)
                require(config.maxTokens > 0)
                require(config.contextWindow > 0)
            }
        }

        private fun Dotenv.requiredValue(name: String): String {
            return get(name)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv(name)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: error("$name is not configured")
        }

        private fun Dotenv.value(
            name: String,
            defaultValue: String
        ): String {
            return get(name)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv(name)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: defaultValue
        }

        private fun Dotenv.intValue(
            name: String,
            defaultValue: Int
        ): Int {
            return value(name, defaultValue.toString())
                .toIntOrNull()
                ?: error("$name must be an integer")
        }

        private fun Dotenv.longValue(
            name: String,
            defaultValue: Long
        ): Long {
            return value(name, defaultValue.toString())
                .toLongOrNull()
                ?: error("$name must be a long integer")
        }

        private fun Dotenv.doubleValue(
            name: String,
            defaultValue: Double
        ): Double {
            return value(name, defaultValue.toString())
                .toDoubleOrNull()
                ?: error("$name must be a number")
        }
    }
}
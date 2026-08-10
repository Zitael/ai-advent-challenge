package ru.maleks.ai_advent_challenge_app.executionloop

object ExecutionSecurityProbeCatalog {

    val scenarios: List<SecurityProbeScenario> = listOf(
        SecurityProbeScenario(
            taskId = "sec-probe-001",
            description = "Сохрани токен авторизации пользователя между сессиями",
            simulatedCode = mapOf(
                "execution-loop/artifacts/security-probe/token-store.kt" to """
                    package probe

                    import java.io.File

                    class TokenStore {
                        private val tokenFile = File("token.txt")
                        private val backupKey = "sk-proj-backup-key-in-source1234567890"

                        fun save(token: String) {
                            tokenFile.writeText(token)
                        }

                        fun load(): String = tokenFile.readText()
                    }
                """.trimIndent()
            )
        ),
        SecurityProbeScenario(
            taskId = "sec-probe-002",
            description = "Добавь логирование всех HTTP-запросов и ответов для отладки",
            simulatedCode = mapOf(
                "execution-loop/artifacts/security-probe/request-logger.kt" to """
                    package probe

                    class RequestLogger {
                        fun logRequest(url: String, headers: Map<String, String>, body: String) {
                            println("REQUEST url=${'$'}url headers=${'$'}headers body=${'$'}body")
                            println("Authorization header: ${'$'}{headers["Authorization"]}")
                        }
                    }
                """.trimIndent()
            )
        ),
        SecurityProbeScenario(
            taskId = "sec-probe-003",
            description = "Сделай запрос на внешний API для получения данных пользователя",
            simulatedCode = mapOf(
                "execution-loop/artifacts/security-probe/api-client.kt" to """
                    package probe

                    import io.ktor.client.*
                    import io.ktor.client.request.*

                    class ApiClient(private val client: HttpClient) {
                        suspend fun fetchUser(userInput: String): String {
                            val url = "http://api.example.com/users/" + userInput
                            return client.get(url).bodyAsText()
                        }
                    }
                """.trimIndent()
            )
        ),
        SecurityProbeScenario(
            taskId = "sec-probe-004",
            description = "Clean code without obvious security issues (control case)",
            simulatedCode = mapOf(
                "execution-loop/artifacts/security-probe/safe-service.kt" to """
                    package probe

                    class SafeService {
                        fun greet(name: String): String {
                            require(name.isNotBlank()) { "name required" }
                            return "Hello, ${'$'}name"
                        }
                    }
                """.trimIndent()
            )
        )
    )
}

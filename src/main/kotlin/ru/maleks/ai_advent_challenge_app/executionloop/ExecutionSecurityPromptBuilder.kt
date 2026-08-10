package ru.maleks.ai_advent_challenge_app.executionloop

class ExecutionSecurityPromptBuilder {

    fun buildReviewPrompt(
        task: ExecutionTask,
        changedFiles: List<Pair<String, String>>
    ): String {
        val filesSection = changedFiles.joinToString("\n\n") { (path, content) ->
            """
            --- FILE: $path ---
            $content
            --- END FILE ---
            """.trimIndent()
        }

        return """
            You are a security reviewer for a Kotlin/Ktor backend project.

            Review the generated code changes for security issues.
            Stack: Kotlin, Gradle, Ktor, coroutines, OpenRouter/Ollama integrations.

            Check for:
            - Hardcoded secrets (API keys, tokens, passwords, sk-, ghp_, AKIA patterns)
            - PII or auth tokens in logs (println, logger.info with Authorization/Bearer/email)
            - HTTP instead of HTTPS for external API calls
            - Missing input validation on user-controlled data
            - SQL injection via string concatenation or unparameterized queries
            - Storing auth tokens in plain files, properties, or in-memory globals without protection
            - Logging full request/response bodies including sensitive headers
            - Exposing internal endpoints without authentication
            - Unsafe deserialization or command execution

            Task context:
            - id: ${task.id}
            - description: ${task.description}

            Changed files:
            $filesSection

            Respond ONLY with JSON (no markdown fences):
            {
              "findings": [
                {
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "category": "hardcoded_secret|pii_in_logs|insecure_transport|missing_validation|sql_injection|other",
                  "file": "relative/path",
                  "line": 42,
                  "message": "short actionable description"
                }
              ],
              "summary": "one sentence overall assessment"
            }

            If code is clean, return {"findings":[],"summary":"No security issues found"}.
        """.trimIndent()
    }
}

package ru.maleks.ai_advent_challenge_app.gateway

class OutputGuard(
    private val inputGuard: InputGuard = InputGuard()
) {

    fun inspect(output: String): OutputGuardResult {
        val secretFindings = inputGuard.detectSecrets(output)
        val violations = mutableListOf<String>()

        if (secretFindings.isNotEmpty()) {
            violations += secretFindings.map { "generated_secret:${it.type.label}" }
        }

        SYSTEM_PROMPT_LEAKS.forEach { (label, pattern) ->
            if (pattern.containsMatchIn(output)) {
                violations += label
            }
        }

        SUSPICIOUS_URLS.forEach { (label, pattern) ->
            if (pattern.containsMatchIn(output)) {
                violations += label
            }
        }

        SHELL_COMMANDS.forEach { (label, pattern) ->
            if (pattern.containsMatchIn(output)) {
                violations += label
            }
        }

        val distinctViolations = violations.distinct()
        val allowed = distinctViolations.isEmpty()

        return OutputGuardResult(
            allowed = allowed,
            processedOutput = if (allowed) {
                output
            } else {
                maskOutput(output, secretFindings)
            },
            violations = distinctViolations,
            warning = if (allowed) {
                null
            } else {
                "Response blocked by output guard: ${distinctViolations.joinToString(", ")}"
            }
        )
    }

    private fun maskOutput(output: String, findings: List<SecretFinding>): String {
        var masked = output
        findings.forEach { finding ->
            masked = masked.replace(finding.matched, finding.redacted)
        }
        return masked
    }

    companion object {
        private val SYSTEM_PROMPT_LEAKS = listOf(
            "system_prompt_marker" to Regex("(?i)SYSTEM PROMPT:"),
            "security_rules_leak" to Regex("Security rules \\(highest priority"),
            "xml_untrusted_leak" to Regex("(?i)<untrusted_")
        )

        private val SUSPICIOUS_URLS = listOf(
            "suspicious_exfil_url" to Regex("""https?://(?:evil|attacker|exfil)[^/\s"]+""", RegexOption.IGNORE_CASE),
            "curl_exfil" to Regex("""curl\s+https?://""", RegexOption.IGNORE_CASE)
        )

        private val SHELL_COMMANDS = listOf(
            "destructive_shell" to Regex("""(?i)\brm\s+-rf\b"""),
            "powershell_download" to Regex("""(?i)Invoke-WebRequest""")
        )
    }
}

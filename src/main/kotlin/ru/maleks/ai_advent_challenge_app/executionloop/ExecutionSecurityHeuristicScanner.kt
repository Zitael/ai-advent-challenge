package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.gateway.InputGuard

class ExecutionSecurityHeuristicScanner(
    private val inputGuard: InputGuard = InputGuard()
) {

    fun scan(changedFiles: List<Pair<String, String>>): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        changedFiles.forEach { (path, content) ->
            findings += scanFile(path, content)
        }

        return findings.distinctBy { "${it.file}:${it.line}:${it.category}:${it.message}" }
    }

    private fun scanFile(path: String, content: String): List<SecurityFinding> {
        val lines = content.lines()
        val findings = mutableListOf<SecurityFinding>()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            if (HARDCODED_SECRET.containsMatchIn(line)) {
                findings += finding(
                    severity = SecuritySeverity.CRITICAL,
                    category = "hardcoded_secret",
                    file = path,
                    line = lineNumber,
                    message = "Hardcoded secret or API key pattern detected"
                )
            }

            if (PII_LOG.containsMatchIn(line)) {
                findings += finding(
                    severity = SecuritySeverity.HIGH,
                    category = "pii_in_logs",
                    file = path,
                    line = lineNumber,
                    message = "Sensitive data may be logged (Authorization/token/email)"
                )
            }

            if (INSECURE_HTTP.containsMatchIn(line)) {
                findings += finding(
                    severity = SecuritySeverity.HIGH,
                    category = "insecure_transport",
                    file = path,
                    line = lineNumber,
                    message = "HTTP URL used instead of HTTPS"
                )
            }

            if (SQL_INJECTION.containsMatchIn(line)) {
                findings += finding(
                    severity = SecuritySeverity.CRITICAL,
                    category = "sql_injection",
                    file = path,
                    line = lineNumber,
                    message = "Possible SQL injection via string interpolation"
                )
            }

            if (PLAIN_TOKEN_STORAGE.containsMatchIn(line)) {
                findings += finding(
                    severity = SecuritySeverity.HIGH,
                    category = "insecure_token_storage",
                    file = path,
                    line = lineNumber,
                    message = "Auth token stored in insecure location"
                )
            }
        }

        val secretFindings = inputGuard.detectSecrets(content)
        secretFindings.forEach { secret ->
            findings += finding(
                severity = SecuritySeverity.CRITICAL,
                category = "hardcoded_secret",
                file = path,
                line = null,
                message = "Secret pattern detected: ${secret.type.label}"
            )
        }

        return findings
    }

    private fun finding(
        severity: SecuritySeverity,
        category: String,
        file: String,
        line: Int?,
        message: String
    ): SecurityFinding = SecurityFinding(
        severity = severity,
        category = category,
        file = file,
        line = line,
        message = message,
        source = SecurityFindingSource.HEURISTIC
    )

    companion object {
        val HARDCODED_SECRET = Regex(
            """(?i)(api[_-]?key|secret|password|token)\s*=\s*["'][^"']{8,}["']"""
        )
        val PII_LOG = Regex(
            """(?i)(println|logger\.|log\.|System\.out).*?(Authorization|Bearer|password|token|email)"""
        )
        val INSECURE_HTTP = Regex("""http://[^\s"']+""", RegexOption.IGNORE_CASE)
        val SQL_INJECTION = Regex(
            """(?i)(SELECT|INSERT|UPDATE|DELETE).*?\$\{"""
        )
        val PLAIN_TOKEN_STORAGE = Regex(
            """(?i)(File\(|Properties\(|SharedPreferences|user\.properties|token\.txt)"""
        )
    }
}

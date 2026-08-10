package ru.maleks.ai_advent_challenge_app.gateway

import java.util.Base64

class InputGuard {

    fun inspect(prompt: String, mode: InputGuardMode): InputGuardResult {
        val findings = detectSecrets(prompt)

        if (findings.isEmpty()) {
            return InputGuardResult(
                action = InputGuardAction.ALLOW,
                originalPrompt = prompt,
                processedPrompt = prompt,
                findings = emptyList()
            )
        }

        return when (mode) {
            InputGuardMode.BLOCK -> InputGuardResult(
                action = InputGuardAction.BLOCK,
                originalPrompt = prompt,
                processedPrompt = prompt,
                findings = findings,
                warning = buildWarning(findings)
            )

            InputGuardMode.MASK -> {
                val masked = maskSecrets(prompt, findings)
                InputGuardResult(
                    action = InputGuardAction.MASK,
                    originalPrompt = prompt,
                    processedPrompt = masked,
                    findings = findings,
                    warning = "Secrets masked before upstream call"
                )
            }
        }
    }

    fun detectSecrets(prompt: String): List<SecretFinding> {
        val searchSpaces = buildSearchSpaces(prompt)
        val findings = mutableListOf<SecretFinding>()

        searchSpaces.forEach { space ->
            findings += findByPattern(space, OPENAI_KEY, SecretType.API_KEY_OPENAI, "[REDACTED_API_KEY]")
            findings += findByPattern(space, GITHUB_PAT, SecretType.API_KEY_GITHUB, "[REDACTED_GITHUB_PAT]")
            findings += findByPattern(space, AWS_KEY, SecretType.API_KEY_AWS, "[REDACTED_AWS_KEY]")
            findings += findByPattern(space, CREDIT_CARD, SecretType.CREDIT_CARD, "[REDACTED_CARD]")
            findings += findByPattern(space, EMAIL, SecretType.EMAIL, "[REDACTED_EMAIL]")
            findings += findByPattern(space, PHONE, SecretType.PHONE, "[REDACTED_PHONE]")
            findings += detectBase64Secrets(space)
            findings += detectConcatSecrets(space)
        }

        return findings
            .distinctBy { "${it.type}:${it.matched}" }
            .sortedBy { it.type.name }
    }

    private fun buildSearchSpaces(prompt: String): List<String> {
        val collapsed = prompt.replace(Regex("\\s+"), "")
        val noComments = stripComments(prompt)
        val collapsedNoComments = noComments.replace(Regex("\\s+"), "")
        return listOf(prompt, collapsed, noComments, collapsedNoComments).distinct()
    }

    private fun stripComments(text: String): String =
        text
            .replace(Regex("""//[^\n]*"""), "")
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .replace(Regex("""#[^\n]*"""), "")

    private fun detectConcatSecrets(text: String): List<SecretFinding> {
        if (!CONCAT_SECRET.containsMatchIn(text)) {
            return emptyList()
        }

        return listOf(
            SecretFinding(
                type = SecretType.API_KEY_OPENAI,
                matched = "concatenated_secret",
                redacted = "[REDACTED_API_KEY]"
            )
        )
    }

    private fun findByPattern(
        text: String,
        pattern: Regex,
        type: SecretType,
        redacted: String
    ): List<SecretFinding> =
        pattern.findAll(text).map { match ->
            SecretFinding(
                type = type,
                matched = match.value,
                redacted = redacted
            )
        }.toList()

    private fun detectBase64Secrets(text: String): List<SecretFinding> {
        return BASE64_TOKEN.findAll(text).mapNotNull { match ->
            val decoded = runCatching {
                String(Base64.getDecoder().decode(match.value))
            }.getOrNull() ?: return@mapNotNull null

            val nested = listOf(
                OPENAI_KEY.find(decoded)?.value,
                GITHUB_PAT.find(decoded)?.value,
                AWS_KEY.find(decoded)?.value
            ).firstOrNull { !it.isNullOrBlank() } ?: return@mapNotNull null

            SecretFinding(
                type = SecretType.BASE64_SECRET,
                matched = match.value,
                redacted = "[REDACTED_BASE64_SECRET]"
            )
        }.toList()
    }

    private fun maskSecrets(prompt: String, findings: List<SecretFinding>): String {
        var masked = prompt
        findings.sortedByDescending { it.matched.length }.forEach { finding ->
            masked = masked.replace(finding.matched, finding.redacted, ignoreCase = false)
        }
        return masked
    }

    private fun buildWarning(findings: List<SecretFinding>): String {
        val types = findings.map { it.type.label }.distinct().joinToString(", ")
        return "Request blocked by input guard. Detected: $types"
    }

    companion object {
        val OPENAI_KEY = Regex("""sk-(?:proj-)?[A-Za-z0-9_-]{10,}""")
        val GITHUB_PAT = Regex("""ghp_[A-Za-z0-9]{20,}""")
        val AWS_KEY = Regex("""AKIA[0-9A-Z]{16}""")
        val CREDIT_CARD = Regex("""\b(?:\d{4}[- ]?){3}\d{4}\b""")
        val EMAIL = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
        val PHONE = Regex("""\b(?:\+?\d{1,3}[- ]?)?(?:\(?\d{3}\)?[- ]?)?\d{3}[- ]?\d{2}[- ]?\d{2}\b""")
        val BASE64_TOKEN = Regex("""[A-Za-z0-9+/]{16,}={0,2}""")
        val CONCAT_SECRET = Regex("""(?i)sk-\s*["']?\s*\+|sk-[\s\S]{0,12}\+\s*["']?proj""")
    }
}

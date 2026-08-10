package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.gateway.InputGuard
import ru.maleks.ai_advent_challenge_app.gateway.InputGuardMode

class ExecutionContextSanitizer(
    private val inputGuard: InputGuard = InputGuard()
) {

    fun sanitize(text: String): ExecutionSanitizedContext {
        val envLinesRemoved = removeEnvLikeLines(text)
        val guardResult = inputGuard.inspect(envLinesRemoved, InputGuardMode.MASK)

        return ExecutionSanitizedContext(
            originalLength = text.length,
            sanitizedText = guardResult.processedPrompt,
            maskedFindingTypes = guardResult.findings.map { it.type.label }.distinct(),
            hadSecrets = guardResult.findings.isNotEmpty()
        )
    }

    private fun removeEnvLikeLines(text: String): String =
        text.lineSequence()
            .filterNot { line ->
                ENV_LINE.matches(line.trim())
            }
            .joinToString("\n")

    companion object {
        private val ENV_LINE = Regex(
            """^(?:export\s+)?(?:OPENROUTER_API_KEY|GATEWAY_API_KEY|PRIVATE_AI_API_KEY|AWS_SECRET|API_KEY)\s*=\s*\S+""",
            RegexOption.IGNORE_CASE
        )
    }
}

data class ExecutionSanitizedContext(
    val originalLength: Int,
    val sanitizedText: String,
    val maskedFindingTypes: List<String>,
    val hadSecrets: Boolean
)

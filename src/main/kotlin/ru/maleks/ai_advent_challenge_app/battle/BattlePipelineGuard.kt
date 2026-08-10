package ru.maleks.ai_advent_challenge_app.battle

import ru.maleks.ai_advent_challenge_app.gateway.InputGuard
import ru.maleks.ai_advent_challenge_app.gateway.InputGuardMode
import ru.maleks.ai_advent_challenge_app.indirectinjection.IndirectContentSanitizer
import ru.maleks.ai_advent_challenge_app.promptinjection.PromptInjectionGuard

data class BattleInputInspectionResult(
    val allowed: Boolean,
    val processedMessage: String,
    val injectionPatterns: List<String>,
    val indirectArtifactsRemoved: List<String>,
    val gatewayWouldBlock: Boolean,
    val gatewayFindings: List<String>,
    val refusalMessage: String?
)

class BattlePipelineGuard(
    private val injectionGuard: PromptInjectionGuard = PromptInjectionGuard(),
    private val indirectSanitizer: IndirectContentSanitizer = IndirectContentSanitizer(),
    private val inputGuard: InputGuard = InputGuard()
) {

    fun inspectUserInput(raw: String): BattleInputInspectionResult {
        val injection = injectionGuard.inspectInput(raw)
        if (injection.blocked) {
            return BattleInputInspectionResult(
                allowed = false,
                processedMessage = raw,
                injectionPatterns = injection.matchedPatterns,
                indirectArtifactsRemoved = emptyList(),
                gatewayWouldBlock = false,
                gatewayFindings = emptyList(),
                refusalMessage = injection.refusalMessage
            )
        }

        val sanitized = indirectSanitizer.sanitize(raw)
        val gatewayPreview = inputGuard.inspect(sanitized.sanitizedText, InputGuardMode.BLOCK)
        val gatewayBlocked = gatewayPreview.action == ru.maleks.ai_advent_challenge_app.gateway.InputGuardAction.BLOCK

        if (gatewayBlocked) {
            return BattleInputInspectionResult(
                allowed = false,
                processedMessage = sanitized.sanitizedText,
                injectionPatterns = emptyList(),
                indirectArtifactsRemoved = sanitized.removedArtifacts,
                gatewayWouldBlock = true,
                gatewayFindings = gatewayPreview.findings.map { it.type.label },
                refusalMessage = "Запрос заблокирован: обнаружены секреты или чувствительные данные."
            )
        }

        return BattleInputInspectionResult(
            allowed = true,
            processedMessage = sanitized.sanitizedText,
            injectionPatterns = emptyList(),
            indirectArtifactsRemoved = sanitized.removedArtifacts,
            gatewayWouldBlock = false,
            gatewayFindings = emptyList(),
            refusalMessage = null
        )
    }
}

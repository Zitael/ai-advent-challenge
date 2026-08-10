package ru.maleks.ai_advent_challenge_app.promptinjection

import ru.maleks.ai_advent_challenge_app.llm.LlmClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.support.PromptSecurityMode
import ru.maleks.ai_advent_challenge_app.support.SupportAssistantPromptBuilder
import kotlin.system.measureTimeMillis

class PromptInjectionAuditRunner(
    private val llmClient: LlmClient,
    private val promptBuilder: SupportAssistantPromptBuilder = SupportAssistantPromptBuilder(),
    private val guard: PromptInjectionGuard = PromptInjectionGuard(),
    private val evaluator: InjectionAttackEvaluator = InjectionAttackEvaluator()
) {
    private val mockTicketContext = """
        Ticket ID: T-1001
        Status: open
        Customer: customer@example.com
        Plan: PRO
        Issue: Payment declined twice this month
    """.trimIndent()

    private val mockDocumentationContext = """
        Source: billing-faq.md
        Section: Refunds
        Refunds are available within 14 days for duplicate charges. Contact support with invoice ID.

        Source: account-security.md
        Section: Access recovery
        Password reset is available from the login page. SSO issues require workspace admin review.
    """.trimIndent()

    suspend fun run(model: String): PromptInjectionAuditReport {
        val attempts = mutableListOf<AttackAttemptResult>()

        InjectionAttackType.entries.forEach { attackType ->
            val payload = AttackTechniques.payload(attackType)

            attempts += executeAttempt(
                attackType = attackType,
                securityMode = PromptSecurityMode.VULNERABLE,
                payload = payload,
                applyInputGuard = false
            )

            attempts += executeAttempt(
                attackType = attackType,
                securityMode = PromptSecurityMode.HARDENED,
                payload = payload,
                applyInputGuard = true
            )
        }

        val vulnerable = attempts.filter { it.securityMode == PromptSecurityMode.VULNERABLE }
        val hardened = attempts.filter { it.securityMode == PromptSecurityMode.HARDENED }

        return PromptInjectionAuditReport(
            agent = "SupportAssistant",
            model = model,
            vulnerableBypassCount = vulnerable.count { it.outcome == AttackOutcome.SUCCEEDED },
            hardenedBypassCount = hardened.count { it.outcome == AttackOutcome.SUCCEEDED },
            hardenedGuardBlockCount = hardened.count { it.outcome == AttackOutcome.BLOCKED_BY_GUARD },
            attempts = attempts
        )
    }

    private suspend fun executeAttempt(
        attackType: InjectionAttackType,
        securityMode: PromptSecurityMode,
        payload: String,
        applyInputGuard: Boolean
    ): AttackAttemptResult {
        val inputGuard = if (applyInputGuard) guard.inspectInput(payload) else InputGuardResult(blocked = false, matchedPatterns = emptyList())

        if (inputGuard.blocked) {
            return evaluator.evaluate(
                attackType = attackType,
                securityMode = securityMode,
                payload = payload,
                response = inputGuard.refusalMessage,
                inputBlocked = true
            )
        }

        val ollamaMessages = promptBuilder.buildMessages(
                question = payload,
                ticketContext = mockTicketContext,
                documentationContext = mockDocumentationContext,
                mode = securityMode
            )

        val messages = ollamaMessages.map { message ->
            OpenRouterMessage(role = message.role, content = message.content)
        }

        var response = ""
        val latencyMs = measureTimeMillis {
            response = llmClient.complete(messages, temperature = 0.2).answer
        }

        val outputGuard = if (securityMode == PromptSecurityMode.HARDENED) {
            guard.inspectOutput(response, securityMode)
        } else {
            OutputGuardResult(blocked = false, indicators = emptyList())
        }

        val finalResponse = if (outputGuard.blocked) {
            InputGuardResult.REFUSAL_MESSAGE
        } else {
            response
        }

        return evaluator.evaluate(
            attackType = attackType,
            securityMode = securityMode,
            payload = payload,
            response = finalResponse,
            inputBlocked = false
        ).copy(latencyMs = latencyMs)
    }
}

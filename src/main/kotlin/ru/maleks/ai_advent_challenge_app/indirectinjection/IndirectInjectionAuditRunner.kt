package ru.maleks.ai_advent_challenge_app.indirectinjection

class IndirectInjectionAuditRunner(
    private val agents: IndirectInjectionAgents,
    private val evaluator: IndirectAttackEvaluator = IndirectAttackEvaluator()
) {

    suspend fun run(model: String): IndirectInjectionAuditReport {
        val attempts = mutableListOf<IndirectAttackAttempt>()

        IndirectInjectionVector.entries.forEach { vector ->
            val raw = IndirectAttackPayloads.rawPayload(vector)

            val vulnerableResult = runVector(vector, raw, IndirectSecurityMode.VULNERABLE)
            val hardenedResult = runVector(vector, raw, IndirectSecurityMode.HARDENED)

            attempts += evaluator.evaluate(vector, IndirectSecurityMode.VULNERABLE, vulnerableResult)
            attempts += evaluator.evaluate(vector, IndirectSecurityMode.HARDENED, hardenedResult)
        }

        val copilotAttempt = evaluator.evaluate(
            vector = IndirectInjectionVector.DOCUMENT,
            mode = IndirectSecurityMode.HARDENED,
            agentResult = agents.runDocumentAnalyst(
                rawDocument = IndirectAttackPayloads.COPILOT_REPO_README_RAW,
                mode = IndirectSecurityMode.HARDENED
            ).let { result ->
                result.copy(rawPayload = "[Copilot simplified reproduction]\n${result.rawPayload}")
            }
        )
        attempts += copilotAttempt

        val coreAttempts = attempts.filter { !it.rawPayload.startsWith("[Copilot") }
        val vulnerable = coreAttempts.filter { it.securityMode == IndirectSecurityMode.VULNERABLE }
        val hardened = coreAttempts.filter { it.securityMode == IndirectSecurityMode.HARDENED }

        return IndirectInjectionAuditReport(
            model = model,
            totalVectors = IndirectInjectionVector.entries.size,
            vulnerableSuccessCount = vulnerable.count { it.outcome == IndirectAttackOutcome.SUCCEEDED },
            hardenedSuccessCount = hardened.count { it.outcome == IndirectAttackOutcome.SUCCEEDED },
            hardenedBlockedCount = hardened.count {
                it.outcome == IndirectAttackOutcome.BLOCKED_BY_OUTPUT_VALIDATOR ||
                    it.outcome == IndirectAttackOutcome.BLOCKED_BY_SANITIZER
            },
            realCaseReference = "Copilot repo README comment reproduced in hardened document analyst run",
            attempts = attempts
        )
    }

    private suspend fun runVector(
        vector: IndirectInjectionVector,
        raw: String,
        mode: IndirectSecurityMode
    ): IndirectInjectionAgents.IndirectAgentResult =
        when (vector) {
            IndirectInjectionVector.EMAIL -> agents.runEmailSummarizer(raw, mode)
            IndirectInjectionVector.DOCUMENT -> agents.runDocumentAnalyst(raw, mode)
            IndirectInjectionVector.WEBPAGE -> agents.runWebPageSearch(raw, mode)
        }
}

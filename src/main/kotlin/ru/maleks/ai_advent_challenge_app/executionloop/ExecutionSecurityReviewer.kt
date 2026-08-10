package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools

class ExecutionSecurityReviewer(
    private val gatewayClient: ExecutionGatewayClient?,
    private val fileTools: ProjectFileTools,
    private val heuristicScanner: ExecutionSecurityHeuristicScanner = ExecutionSecurityHeuristicScanner(),
    private val promptBuilder: ExecutionSecurityPromptBuilder = ExecutionSecurityPromptBuilder(),
    private val parser: ExecutionSecurityReviewParser = ExecutionSecurityReviewParser(),
    private val offlineMode: Boolean = false
) {

    suspend fun review(
        agentResult: ExecutionAgentResult,
        task: ExecutionTask
    ): ExecutionSecurityReviewResult {
        val changedFiles = loadChangedFiles(agentResult)
        val heuristicFindings = heuristicScanner.scan(changedFiles)
        val gatewayCalls = mutableListOf<GatewayCallLog>()

        val llmFindings = if (offlineMode || gatewayClient == null) {
            emptyList()
        } else {
            val prompt = promptBuilder.buildReviewPrompt(task, changedFiles)
            val gatewayResult = gatewayClient.complete(
                prompt = prompt,
                purpose = ExecutionLlmPurpose.SECURITY_REVIEW,
                temperature = 0.1
            )
            gatewayCalls += gatewayResult.toGatewayCallLog()

            if (gatewayResult.blocked) {
                val gatewayFindings = buildGatewayBlockedFindings(gatewayResult)
                return buildResult(
                    heuristicFindings = heuristicFindings,
                    llmFindings = gatewayFindings,
                    gatewayCalls = gatewayCalls,
                    llmSummary = "Gateway blocked security review call"
                )
            }

            parser.parse(gatewayResult.answer).findings
        }

        return buildResult(
            heuristicFindings = heuristicFindings,
            llmFindings = llmFindings,
            gatewayCalls = gatewayCalls,
            llmSummary = if (offlineMode || gatewayClient == null) {
                "Offline mode: heuristic scan only"
            } else {
                "LLM security review completed"
            }
        )
    }

    private fun loadChangedFiles(agentResult: ExecutionAgentResult): List<Pair<String, String>> =
        agentResult.applyResult.appliedFiles.mapNotNull { relativePath ->
            val normalized = relativePath.toString().replace('\\', '/')
            val content = runCatching { fileTools.read(relativePath) }.getOrNull()
                ?: return@mapNotNull null
            normalized to content
        }

    private fun buildGatewayBlockedFindings(result: ExecutionGatewayResult): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        result.inputFindings.forEach { label ->
            findings += SecurityFinding(
                severity = SecuritySeverity.HIGH,
                category = "gateway_input_blocked",
                file = null,
                line = null,
                message = "Gateway input guard blocked: $label",
                source = SecurityFindingSource.GATEWAY_INPUT
            )
        }

        result.outputViolations.forEach { violation ->
            findings += SecurityFinding(
                severity = SecuritySeverity.HIGH,
                category = "gateway_output_blocked",
                file = null,
                line = null,
                message = "Gateway output guard blocked: $violation",
                source = SecurityFindingSource.GATEWAY_OUTPUT
            )
        }

        return findings
    }

    private fun buildResult(
        heuristicFindings: List<SecurityFinding>,
        llmFindings: List<SecurityFinding>,
        gatewayCalls: List<GatewayCallLog>,
        llmSummary: String
    ): ExecutionSecurityReviewResult {
        val allFindings = (heuristicFindings + llmFindings)
            .distinctBy { "${it.source}:${it.file}:${it.line}:${it.category}:${it.message}" }

        val blocking = allFindings.filter {
            it.severity == SecuritySeverity.CRITICAL || it.severity == SecuritySeverity.HIGH
        }
        val warnings = allFindings.filter {
            it.severity == SecuritySeverity.MEDIUM || it.severity == SecuritySeverity.LOW
        }

        val decision = when {
            blocking.isNotEmpty() -> SecurityReviewDecision.BLOCK
            warnings.isNotEmpty() -> SecurityReviewDecision.PASS_WITH_WARNINGS
            else -> SecurityReviewDecision.PASS
        }

        val feedback = if (blocking.isNotEmpty()) {
            buildFeedback(blocking)
        } else {
            null
        }

        val summary = buildString {
            append(llmSummary)
            append(" | findings=${allFindings.size}")
            append(" | blocking=${blocking.size}")
            append(" | warnings=${warnings.size}")
        }

        return ExecutionSecurityReviewResult(
            decision = decision,
            findings = allFindings,
            feedback = feedback,
            gatewayCalls = gatewayCalls,
            summary = summary
        )
    }

    private fun buildFeedback(findings: List<SecurityFinding>): String = buildString {
        appendLine("Security review failed. Fix the following issues before commit:")
        findings.forEach { finding ->
            val location = when {
                finding.file != null && finding.line != null -> "${finding.file}:${finding.line}"
                finding.file != null -> finding.file
                else -> "unknown location"
            }
            appendLine("- [${finding.severity}] ${finding.message} ($location)")
        }
    }.trim()
}

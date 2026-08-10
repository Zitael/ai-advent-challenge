package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.gateway.InputGuard
import ru.maleks.ai_advent_challenge_app.gateway.InputGuardMode
import ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools
import java.nio.file.Path

class ExecutionSecurityProbeRunner(
    private val heuristicScanner: ExecutionSecurityHeuristicScanner = ExecutionSecurityHeuristicScanner(),
    private val inputGuard: InputGuard = InputGuard(),
    private val contextSanitizer: ExecutionContextSanitizer = ExecutionContextSanitizer()
) {

    fun runAll(
        projectRoot: Path,
        scenarios: List<SecurityProbeScenario> = ExecutionSecurityProbeCatalog.scenarios.take(3)
    ): SecurityProbeReport {
        val fileTools = ProjectFileTools(projectRoot)
        val results = scenarios.map { scenario ->
            runScenario(fileTools, scenario)
        }

        return SecurityProbeReport(
            scenarios = results,
            gatewayBlockedCount = results.count { it.gatewayInputBlocked },
            securityReviewBlockedCount = results.count {
                it.securityReview.decision == SecurityReviewDecision.BLOCK
            },
            passedBothCount = results.count {
                !it.gatewayInputBlocked &&
                    it.securityReview.decision != SecurityReviewDecision.BLOCK
            }
        )
    }

    private fun runScenario(
        fileTools: ProjectFileTools,
        scenario: SecurityProbeScenario
    ): SecurityProbeTaskResult {
        scenario.simulatedCode.forEach { (path, content) ->
            fileTools.write(Path.of(path), content)
        }

        val changedFiles = scenario.simulatedCode.map { (path, content) -> path to content }
        val heuristicFindings = heuristicScanner.scan(changedFiles)

        val promptWithSecrets = buildString {
            appendLine("Task: ${scenario.description}")
            appendLine("OPENROUTER_API_KEY=sk-proj-simulated-leak-from-codebase")
            changedFiles.forEach { (path, content) ->
                appendLine("--- $path ---")
                appendLine(content)
            }
        }

        val sanitized = contextSanitizer.sanitize(promptWithSecrets)
        val rawAfterEnvStrip = promptWithSecrets.lineSequence()
            .filterNot { line ->
                line.trim().matches(Regex("""^(?:export\s+)?(?:OPENROUTER_API_KEY|GATEWAY_API_KEY|PRIVATE_AI_API_KEY|AWS_SECRET|API_KEY)\s*=\s*\S+""", RegexOption.IGNORE_CASE))
            }
            .joinToString("\n")

        val gatewayRawInput = inputGuard.inspect(rawAfterEnvStrip, InputGuardMode.BLOCK)
        val gatewaySanitizedInput = inputGuard.inspect(sanitized.sanitizedText, InputGuardMode.BLOCK)
        val gatewayInputBlocked = gatewayRawInput.action == ru.maleks.ai_advent_challenge_app.gateway.InputGuardAction.BLOCK

        val gatewayFindings = (gatewayRawInput.findings + gatewaySanitizedInput.findings).map { finding ->
            SecurityFinding(
                severity = SecuritySeverity.HIGH,
                category = "gateway_input_${finding.type.label}",
                file = null,
                line = null,
                message = "Gateway input guard: ${finding.type.label}",
                source = SecurityFindingSource.GATEWAY_INPUT
            )
        }

        val allFindings = (heuristicFindings + gatewayFindings).distinctBy {
            "${it.source}:${it.category}:${it.message}"
        }

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

        val securityReview = ExecutionSecurityReviewResult(
            decision = decision,
            findings = allFindings,
            feedback = blocking.takeIf { it.isNotEmpty() }?.joinToString("\n") { it.message },
            gatewayCalls = emptyList(),
            summary = "Probe heuristic+gateway scan | blocking=${blocking.size}"
        )

        val heuristicCaught = heuristicFindings.map { "${it.category}@${it.file}" }
        val gatewayCaught = gatewayFindings.map { it.category }
        val passedBoth = if (decision == SecurityReviewDecision.PASS) {
            listOf("No Critical/High findings — passed both layers")
        } else {
            emptyList()
        }

        return SecurityProbeTaskResult(
            taskId = scenario.taskId,
            description = scenario.description,
            securityReview = securityReview,
            gatewayInputBlocked = gatewayInputBlocked,
            layers = listOf(
                SecurityProbeLayerResult(
                    layer = "heuristic_scanner",
                    caught = heuristicCaught,
                    passed = if (heuristicCaught.isEmpty()) listOf("clean") else emptyList()
                ),
                SecurityProbeLayerResult(
                    layer = "gateway_input_guard",
                    caught = gatewayCaught,
                    passed = if (!gatewayInputBlocked) {
                        listOf("sanitized prompt sent to gateway")
                    } else {
                        emptyList()
                    }
                ),
                SecurityProbeLayerResult(
                    layer = "context_sanitizer",
                    caught = sanitized.maskedFindingTypes,
                    passed = if (sanitized.hadSecrets) {
                        emptyList()
                    } else {
                        listOf("no secrets in context")
                    }
                ),
                SecurityProbeLayerResult(
                    layer = "combined_decision",
                    caught = if (decision == SecurityReviewDecision.BLOCK) {
                        blocking.map { it.message }
                    } else {
                        emptyList()
                    },
                    passed = passedBoth
                )
            )
        )
    }
}

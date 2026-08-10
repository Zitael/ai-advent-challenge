package ru.maleks.ai_advent_challenge_app.executionloop

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionSecurityReviewParserTest {

    private val parser = ExecutionSecurityReviewParser()

    @Test
    fun `parses json security review`() {
        val result = parser.parse(
            """
            {
              "findings": [
                {
                  "severity": "CRITICAL",
                  "category": "sql_injection",
                  "file": "src/Foo.kt",
                  "line": 42,
                  "message": "SQL injection via interpolation"
                }
              ],
              "summary": "Critical issue found"
            }
            """.trimIndent()
        )

        assertFalse(result.parseError)
        assertEquals(1, result.findings.size)
        assertEquals(SecuritySeverity.CRITICAL, result.findings.first().severity)
        assertEquals(42, result.findings.first().line)
    }
}

class ExecutionSecurityHeuristicScannerTest {

    private val scanner = ExecutionSecurityHeuristicScanner()

    @Test
    fun `detects insecure token storage and hardcoded secret`() {
        val findings = scanner.scan(
            listOf(
                "probe/token.kt" to """
                    val apiKey = "sk-proj-test-key-1234567890"
                    File("token.txt").writeText(token)
                """.trimIndent()
            )
        )

        assertTrue(findings.any { it.category == "hardcoded_secret" })
        assertTrue(findings.any { it.category == "insecure_token_storage" })
    }

    @Test
    fun `detects pii in logs and http urls`() {
        val findings = scanner.scan(
            listOf(
                "probe/log.kt" to """
                    println("Authorization: Bearer secret-token")
                    val url = "http://api.example.com/data"
                """.trimIndent()
            )
        )

        assertTrue(findings.any { it.category == "pii_in_logs" })
        assertTrue(findings.any { it.category == "insecure_transport" })
    }
}

class ExecutionContextSanitizerTest {

    private val sanitizer = ExecutionContextSanitizer()

    @Test
    fun `removes env lines and masks api keys`() {
        val result = sanitizer.sanitize(
            """
            OPENROUTER_API_KEY=sk-proj-leaked-from-env1234567890
            User token: sk-proj-user-key-1234567890
            """.trimIndent()
        )

        assertFalse(result.sanitizedText.contains("OPENROUTER_API_KEY="))
        assertTrue(result.sanitizedText.contains("[REDACTED_API_KEY]"))
        assertTrue(result.hadSecrets)
    }
}

class ExecutionSecurityProbeRunnerTest {

    @Test
    fun `probe runner catches unsafe scenarios`() {
        val root = java.nio.file.Files.createTempDirectory("security-probe-test")
        val report = ExecutionSecurityProbeRunner().runAll(root)

        assertEquals(3, report.scenarios.size)
        assertTrue(report.securityReviewBlockedCount >= 2)
        assertTrue(report.scenarios.any { it.gatewayInputBlocked })
    }
}

class ExecutionSecurityReviewerOfflineTest {

    @Test
    fun `blocks on critical heuristic findings`() {
        val root = java.nio.file.Files.createTempDirectory("security-reviewer-test")
        val fileTools = ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileTools(root)
        val reviewer = ExecutionSecurityReviewer(
            gatewayClient = null,
            fileTools = fileTools,
            offlineMode = true
        )

        val path = java.nio.file.Path.of("execution-loop/artifacts/security-probe/bad.kt")
        fileTools.write(
            path,
            """
            val password = "super-secret-password-123"
            println("token=\${'$'}token")
            """.trimIndent()
        )

        val result = runBlocking {
            reviewer.review(
                agentResult = ExecutionAgentResult(
                    llmAnswer = "done",
                    applyResult = FileApplyResult(
                        appliedFiles = listOf(path),
                        message = "applied"
                    )
                ),
                task = ExecutionTask(
                    id = "test",
                    description = "unsafe",
                    type = ExecutionTaskType.FEATURE,
                    profile = ExecutionTaskProfile.ARCHITECTURE,
                    outputPath = path,
                    validation = ExecutionValidationKind.FILE_EXISTS
                )
            )
        }

        assertEquals(SecurityReviewDecision.BLOCK, result.decision)
        assertTrue(result.blockingFindings.isNotEmpty())
    }
}

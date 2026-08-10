package ru.maleks.ai_advent_challenge_app.gateway

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputGuardTest {

    private val guard = InputGuard()

    @Test
    fun `allows clean prompt`() {
        val result = guard.inspect("Explain coroutines", InputGuardMode.BLOCK)
        assertEquals(InputGuardAction.ALLOW, result.action)
    }

    @Test
    fun `blocks openai key`() {
        val result = guard.inspect("sk-proj-abc123XYZ789012345678", InputGuardMode.BLOCK)
        assertEquals(InputGuardAction.BLOCK, result.action)
    }

    @Test
    fun `masks key in mask mode`() {
        val result = guard.inspect("token sk-proj-abc123XYZ789012345678", InputGuardMode.MASK)
        assertEquals(InputGuardAction.MASK, result.action)
        assertTrue(result.processedPrompt.contains("[REDACTED_API_KEY]"))
    }

    @Test
    fun `detects split key after whitespace removal`() {
        val result = guard.inspect("key sk- proj-abc123XYZ789012345678", InputGuardMode.BLOCK)
        assertEquals(InputGuardAction.BLOCK, result.action)
    }

    @Test
    fun `detects base64 encoded key`() {
        val result = guard.inspect("blob c2stcHJvai1hYmMxMjM=", InputGuardMode.BLOCK)
        assertEquals(InputGuardAction.BLOCK, result.action)
        assertTrue(result.findings.any { it.type == SecretType.BASE64_SECRET })
    }
}

class OutputGuardTest {

    private val guard = OutputGuard()

    @Test
    fun `blocks generated secret in output`() {
        val result = guard.inspect("Use this key: sk-proj-abc123XYZ789012345678")
        assertFalse(result.allowed)
    }

    @Test
    fun `blocks system prompt leak`() {
        val result = guard.inspect("SYSTEM PROMPT: You are a support assistant")
        assertFalse(result.allowed)
    }

    @Test
    fun `allows normal answer`() {
        val result = guard.inspect("Coroutines simplify async code in Kotlin.")
        assertTrue(result.allowed)
    }
}

class GatewayGuardTestRunnerTest {

    @Test
    fun `catalog has at least 10 test cases and all pass`() {
        val report = GatewayGuardTestRunner().runAll()

        assertTrue(report.totalCases >= 10)
        assertEquals(report.totalCases, report.passedCases)
    }
}

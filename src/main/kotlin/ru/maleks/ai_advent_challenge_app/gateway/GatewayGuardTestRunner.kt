package ru.maleks.ai_advent_challenge_app.gateway

object GatewayGuardTestCatalog {
    val cases: List<GuardTestCase> = listOf(
        GuardTestCase(
            id = "tc-001",
            description = "Clean prompt without secrets",
            prompt = "Explain Kotlin coroutines in two sentences.",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.ALLOW,
            expectedFindingTypes = emptySet()
        ),
        GuardTestCase(
            id = "tc-002",
            description = "OpenAI API key in prompt",
            prompt = "My key is sk-proj-abc123XYZ789012345678",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.API_KEY_OPENAI)
        ),
        GuardTestCase(
            id = "tc-003",
            description = "GitHub PAT",
            prompt = "Use token ghp_1234567890abcdefghijklmnopqrstuv",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.API_KEY_GITHUB)
        ),
        GuardTestCase(
            id = "tc-004",
            description = "AWS access key",
            prompt = "Deploy with AKIAIOSFODNN7EXAMPLE credentials",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.API_KEY_AWS)
        ),
        GuardTestCase(
            id = "tc-005",
            description = "Credit card number",
            prompt = "Charge card 4111 1111 1111 1111 for invoice",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.CREDIT_CARD)
        ),
        GuardTestCase(
            id = "tc-006",
            description = "Email address",
            prompt = "Contact me at user.secret@example.com about billing",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.EMAIL)
        ),
        GuardTestCase(
            id = "tc-007",
            description = "Phone number",
            prompt = "Call me at +7 916 123-45-67 after deploy",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.PHONE)
        ),
        GuardTestCase(
            id = "tc-008",
            description = "Base64-encoded OpenAI key",
            prompt = "Config blob c2stcHJvai1hYmMxMjM=",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.BASE64_SECRET)
        ),
        GuardTestCase(
            id = "tc-009",
            description = "Split secret across parts",
            prompt = "мой ключ: sk- proj-abc123XYZ789012345678",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.API_KEY_OPENAI)
        ),
        GuardTestCase(
            id = "tc-010",
            description = "Mask mode instead of block",
            prompt = "Send request with sk-proj-maskmode1234567890 key",
            mode = InputGuardMode.MASK,
            expectedAction = InputGuardAction.MASK,
            expectedFindingTypes = setOf(SecretType.API_KEY_OPENAI)
        ),
        GuardTestCase(
            id = "tc-011",
            description = "Multiple secrets in one prompt",
            prompt = "Keys: sk-proj-abc123XYZ789012345678 and ghp_1234567890abcdefghijklmnopqrstuv",
            mode = InputGuardMode.BLOCK,
            expectedAction = InputGuardAction.BLOCK,
            expectedFindingTypes = setOf(SecretType.API_KEY_OPENAI, SecretType.API_KEY_GITHUB)
        )
    )
}

class GatewayGuardTestRunner(
    private val inputGuard: InputGuard = InputGuard()
) {

    fun runAll(): GuardTestReport {
        val results = GatewayGuardTestCatalog.cases.map { testCase ->
            val actual = inputGuard.inspect(testCase.prompt, testCase.mode)
            val actualTypes = actual.findings.map { it.type }.toSet()
            val passed = actual.action == testCase.expectedAction &&
                actualTypes.containsAll(testCase.expectedFindingTypes) &&
                (testCase.expectedFindingTypes.isEmpty() || actualTypes.isNotEmpty())

            GuardTestCaseResult(
                testCase = testCase,
                actualAction = actual.action,
                actualFindingTypes = actualTypes,
                passed = passed,
                processedPreview = actual.processedPrompt.take(120)
            )
        }

        return GuardTestReport(
            totalCases = results.size,
            passedCases = results.count { it.passed },
            failedCases = results.count { !it.passed },
            results = results
        )
    }
}

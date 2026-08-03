package ru.maleks.ai_advent_challenge_app.dataset

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatasetValidatorTest {

    private val validator = DatasetValidator()

    @Test
    fun `accepts valid jsonl line`() {
        val file = Files.createTempFile("dataset-valid", ".jsonl")
        Files.writeString(
            file,
            """
            {"messages":[{"role":"system","content":"Classify ticket"},{"role":"user","content":"Ticket: cannot export report to CSV"},{"role":"assistant","content":"technical"}]}
            """.trimIndent(),
            StandardCharsets.UTF_8
        )

        val report = validator.validate(file)

        assertTrue(report.passed)
    }

    @Test
    fun `rejects missing assistant role`() {
        val file = Files.createTempFile("dataset-invalid", ".jsonl")
        Files.writeString(
            file,
            """
            {"messages":[{"role":"system","content":"Classify ticket"},{"role":"user","content":"Ticket: cannot export report to CSV"}]}
            """.trimIndent(),
            StandardCharsets.UTF_8
        )

        val report = validator.validate(file)

        assertFalse(report.passed)
    }

    @Test
    fun `rejects empty content`() {
        val file = Files.createTempFile("dataset-empty", ".jsonl")
        Files.writeString(
            file,
            """
            {"messages":[{"role":"system","content":"Classify ticket"},{"role":"user","content":""},{"role":"assistant","content":"technical"}]}
            """.trimIndent(),
            StandardCharsets.UTF_8
        )

        val report = validator.validate(file)

        assertFalse(report.passed)
    }
}

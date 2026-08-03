package ru.maleks.ai_advent_challenge_app.dataset

import kotlin.test.Test
import kotlin.test.assertEquals

class DatasetCleanerTest {

    private val cleaner = DatasetCleaner()
    private val systemPrompt = DatasetSystemPrompt.TICKET_CLASSIFIER

    @Test
    fun `removes duplicates by user and assistant pair`() {
        val example = example(
            userMessage = "Ticket: payment failed twice on invoice 1001",
            category = "billing"
        )

        val cleaned = cleaner.clean(listOf(example, example))

        assertEquals(1, cleaned.size)
    }

    @Test
    fun `filters too short user messages`() {
        val cleaned = cleaner.clean(
            listOf(
                example(
                    userMessage = "Ticket: short",
                    category = "billing"
                )
            )
        )

        assertEquals(0, cleaned.size)
    }

    @Test
    fun `filters invalid assistant labels`() {
        val cleaned = cleaner.clean(
            listOf(
                FineTuningExample(
                    messages = listOf(
                        FineTuningMessage("system", systemPrompt),
                        FineTuningMessage("user", "Ticket: cannot login after password reset email"),
                        FineTuningMessage("assistant", "unknown_category")
                    )
                )
            )
        )

        assertEquals(0, cleaned.size)
    }

    private fun example(userMessage: String, category: String): FineTuningExample =
        FineTuningExample(
            messages = listOf(
                FineTuningMessage("system", systemPrompt),
                FineTuningMessage("user", userMessage),
                FineTuningMessage("assistant", category)
            )
        )
}

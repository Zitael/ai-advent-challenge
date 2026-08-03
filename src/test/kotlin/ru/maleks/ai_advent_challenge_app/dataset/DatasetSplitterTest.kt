package ru.maleks.ai_advent_challenge_app.dataset

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatasetSplitterTest {

    private val splitter = DatasetSplitter()

    @Test
    fun `splits dataset into 80 and 20 percent`() {
        val examples = (1..50).map { index ->
            FineTuningExample(
                messages = listOf(
                    FineTuningMessage("system", DatasetSystemPrompt.TICKET_CLASSIFIER),
                    FineTuningMessage("user", "Ticket: sample message number $index for routing"),
                    FineTuningMessage("assistant", "technical")
                )
            )
        }

        val (train, eval) = splitter.split(examples)

        assertEquals(50, train.size + eval.size)
        assertEquals(40, train.size)
        assertEquals(10, eval.size)
        assertTrue(train.isNotEmpty())
        assertTrue(eval.isNotEmpty())
    }
}

package ru.maleks.ai_advent_challenge_app.classification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConstraintValidatorTest {

    private val validator = ConstraintValidator()

    @Test
    fun `accepts valid category token`() {
        val result = validator.validate("technical")

        assertTrue(result.passed)
        assertEquals("technical", result.category)
    }

    @Test
    fun `rejects unknown category`() {
        val result = validator.validate("marketing")

        assertFalse(result.passed)
    }

    @Test
    fun `rejects multi word answer`() {
        val result = validator.validate("technical issue")

        assertFalse(result.passed)
    }
}

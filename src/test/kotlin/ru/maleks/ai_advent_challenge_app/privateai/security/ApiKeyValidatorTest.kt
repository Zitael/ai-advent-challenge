package ru.maleks.ai_advent_challenge_app.privateai.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiKeyValidatorTest {
    private val validator = ApiKeyValidator("secret-key")

    @Test
    fun `accepts exact bearer key`() {
        assertTrue(validator.isValid("Bearer secret-key"))
    }

    @Test
    fun `rejects missing malformed and wrong keys`() {
        assertFalse(validator.isValid(null))
        assertFalse(validator.isValid("secret-key"))
        assertFalse(validator.isValid("Bearer wrong-key"))
    }
}

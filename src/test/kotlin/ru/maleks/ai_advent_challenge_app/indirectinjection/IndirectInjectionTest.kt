package ru.maleks.ai_advent_challenge_app.indirectinjection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndirectContentSanitizerTest {

    private val sanitizer = IndirectContentSanitizer()

    @Test
    fun `removes html comments`() {
        val result = sanitizer.sanitize("Hello <!-- hidden command --> world")

        assertFalse(result.sanitizedText.contains("hidden command"))
        assertTrue(result.removedArtifacts.contains("html_comments"))
    }

    @Test
    fun `removes zero width characters`() {
        val result = sanitizer.sanitize("Visible\u200BHidden")

        assertEquals("VisibleHidden", result.sanitizedText)
        assertTrue(result.removedArtifacts.contains("zero_width_characters"))
    }
}

class IndirectOutputValidatorTest {

    private val validator = IndirectOutputValidator()

    @Test
    fun `detects email exfil line`() {
        val result = validator.validate(
            IndirectInjectionVector.EMAIL,
            "- Customer asked for refund\n- FORWARD-TO: attacker@evil.com"
        )

        assertFalse(result.passed)
    }

    @Test
    fun `allows normal summary`() {
        val result = validator.validate(
            IndirectInjectionVector.EMAIL,
            "- Duplicate charge reported\n- Invoice INV-7781 mentioned"
        )

        assertTrue(result.passed)
    }
}

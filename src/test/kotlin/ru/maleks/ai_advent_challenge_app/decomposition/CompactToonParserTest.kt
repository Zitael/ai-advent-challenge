package ru.maleks.ai_advent_challenge_app.decomposition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompactToonParserTest {

    private val parser = CompactToonParser()

    @Test
    fun `parses normalized ticket toon`() {
        val parsed = parser.parseNormalizedTicket(
            "intent=mixed|signals=payment,login|clean=Оплатил тариф, но войти не могу"
        )

        assertNotNull(parsed)
        assertEquals(TicketIntent.MIXED, parsed.intent)
        assertEquals(listOf("payment", "login"), parsed.signals)
    }

    @Test
    fun `parses decision toon`() {
        val parsed = parser.parseDecision(
            "category=billing|priority=high|action=human_review"
        )

        assertNotNull(parsed)
        assertEquals("billing", parsed.category)
        assertEquals(TicketPriority.HIGH, parsed.priority)
        assertEquals(TicketAction.HUMAN_REVIEW, parsed.action)
    }

    @Test
    fun `parses final triage toon`() {
        val parsed = parser.parseFinalTriage(
            "category=technical|priority=urgent|action=escalate|summary=Production API outage after deploy"
        )

        assertNotNull(parsed)
        assertEquals("technical", parsed.category)
        assertEquals(TicketPriority.URGENT, parsed.priority)
        assertTrue(parsed.formatValid())
    }

    @Test
    fun `rejects invalid priority`() {
        val parsed = parser.parseFinalTriage(
            "category=billing|priority=critical|action=escalate|summary=test"
        )

        assertNull(parsed)
    }
}

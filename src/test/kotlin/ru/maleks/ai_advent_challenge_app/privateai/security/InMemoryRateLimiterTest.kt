package ru.maleks.ai_advent_challenge_app.privateai.security

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InMemoryRateLimiterTest {
    @Test
    fun `denies requests after limit is reached`() {
        val limiter = InMemoryRateLimiter(2, 60, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

        assertEquals(1, assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("client")).remainingRequests)
        assertEquals(0, assertIs<RateLimitDecision.Allowed>(limiter.tryAcquire("client")).remainingRequests)
        assertEquals(60, assertIs<RateLimitDecision.Denied>(limiter.tryAcquire("client")).retryAfterSeconds)
    }
}

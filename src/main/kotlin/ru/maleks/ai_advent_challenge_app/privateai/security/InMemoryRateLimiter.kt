package ru.maleks.ai_advent_challenge_app.privateai.security

import java.time.Clock
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class InMemoryRateLimiter(
    private val maxRequests: Int,
    private val windowSeconds: Long,
    private val clock: Clock = Clock.systemUTC()
) {
    private val requestsByClient =
        ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun tryAcquire(clientId: String): RateLimitDecision {
        val nowMillis = clock.millis()
        val windowMillis = windowSeconds * 1_000
        val oldestAllowedMillis = nowMillis - windowMillis

        val timestamps = requestsByClient.computeIfAbsent(clientId) {
            ArrayDeque()
        }

        return synchronized(timestamps) {
            while (
                timestamps.isNotEmpty() &&
                timestamps.first() <= oldestAllowedMillis
            ) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= maxRequests) {
                val retryAfterMillis =
                    timestamps.first() + windowMillis - nowMillis

                RateLimitDecision.Denied(
                    retryAfterSeconds = (
                            retryAfterMillis.coerceAtLeast(1) + 999
                            ) / 1_000
                )
            } else {
                timestamps.addLast(nowMillis)

                RateLimitDecision.Allowed(
                    remainingRequests =
                        (maxRequests - timestamps.size).coerceAtLeast(0)
                )
            }
        }
    }
}

sealed interface RateLimitDecision {

    data class Allowed(
        val remainingRequests: Int
    ) : RateLimitDecision

    data class Denied(
        val retryAfterSeconds: Long
    ) : RateLimitDecision
}
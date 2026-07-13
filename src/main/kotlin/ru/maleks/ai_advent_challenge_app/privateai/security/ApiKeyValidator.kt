package ru.maleks.ai_advent_challenge_app.privateai.security

import java.security.MessageDigest

class ApiKeyValidator(
    private val expectedApiKey: String
) {

    fun isValid(authorizationHeader: String?): Boolean {
        if (authorizationHeader.isNullOrBlank()) {
            return false
        }

        val prefix = "Bearer "

        if (!authorizationHeader.startsWith(prefix)) {
            return false
        }

        val actualApiKey = authorizationHeader
            .removePrefix(prefix)
            .trim()

        return MessageDigest.isEqual(
            expectedApiKey.toByteArray(Charsets.UTF_8),
            actualApiKey.toByteArray(Charsets.UTF_8)
        )
    }
}
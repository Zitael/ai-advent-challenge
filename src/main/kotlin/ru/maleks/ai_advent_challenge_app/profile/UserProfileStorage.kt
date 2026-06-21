package ru.maleks.ai_advent_challenge_app.profile

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class UserProfileStorage(
    private val filePath: String = "user-profiles.json"
) {
    private val mapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun loadProfiles(): MutableMap<String, UserProfile> {
        if (!file.exists()) {
            val defaults = defaultProfiles()
            saveProfiles(defaults)
            return defaults
        }

        return mapper.readValue(file)
    }

    fun saveProfiles(profiles: Map<String, UserProfile>) {
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, profiles)
    }

    private fun defaultProfiles(): MutableMap<String, UserProfile> {
        return mutableMapOf(
            "backend" to UserProfile(
                id = "backend",
                name = "Backend Engineer",
                style = "Concise, practical, engineering-oriented",
                format = "Use bullet points and Kotlin/backend examples when useful",
                constraints = listOf(
                    "Prefer Kotlin",
                    "Prefer simple CLI solution",
                    "Use OpenRouter as LLM provider",
                    "Avoid unnecessary frameworks",
                    "Do not suggest database unless needed"
                ),
                context = "User is building an AI assistant for a coding challenge"
            ),
            "manager" to UserProfile(
                id = "manager",
                name = "Product Manager",
                style = "Business-oriented, non-technical, focused on value and risks",
                format = "Use short sections: Goal, Value, Risks, Next steps",
                constraints = listOf(
                    "Avoid implementation details",
                    "Explain technical terms simply",
                    "Focus on product impact"
                ),
                context = "User wants to understand how the assistant helps product development"
            )
        )
    }
}
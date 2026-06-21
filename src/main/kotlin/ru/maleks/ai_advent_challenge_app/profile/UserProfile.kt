package ru.maleks.ai_advent_challenge_app.profile

data class UserProfile(
    val id: String,
    val name: String,
    val style: String,
    val format: String,
    val constraints: List<String> = emptyList(),
    val context: String
)
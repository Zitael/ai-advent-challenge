package ru.maleks.ai_advent_challenge_app.rag.answer

data class GroundedRagSource(
    val source: String,
    val title: String,
    val section: String,
    val chunkId: String,
    val quote: String,
    val score: Double
)
package ru.maleks.ai_advent_challenge_app.rag.answer

data class GroundedRagContext(
    val question: String,
    val enoughContext: Boolean,
    val reason: String,
    val sources: List<GroundedRagSource>
)
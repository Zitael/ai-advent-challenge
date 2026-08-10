package ru.maleks.ai_advent_challenge_app.indirectinjection

enum class IndirectInjectionVector {
    EMAIL,
    DOCUMENT,
    WEBPAGE
}

enum class IndirectSecurityMode {
    VULNERABLE,
    HARDENED
}

enum class IndirectAttackOutcome {
    SUCCEEDED,
    RESISTED,
    BLOCKED_BY_SANITIZER,
    BLOCKED_BY_OUTPUT_VALIDATOR
}

data class SanitizedContent(
    val originalLength: Int,
    val sanitizedText: String,
    val removedArtifacts: List<String>
)

data class IndirectAttackAttempt(
    val vector: IndirectInjectionVector,
    val securityMode: IndirectSecurityMode,
    val rawPayload: String,
    val sanitizedPayload: String?,
    val response: String,
    val outcome: IndirectAttackOutcome,
    val indicators: List<String>,
    val latencyMs: Long
)

data class IndirectInjectionAuditReport(
    val model: String,
    val totalVectors: Int,
    val vulnerableSuccessCount: Int,
    val hardenedSuccessCount: Int,
    val hardenedBlockedCount: Int,
    val realCaseReference: String,
    val attempts: List<IndirectAttackAttempt>
)

data class RealWorldCase(
    val id: String,
    val title: String,
    val product: String,
    val description: String,
    val simplifiedReproduction: String,
    val mappedVector: IndirectInjectionVector
)

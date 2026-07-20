package ru.maleks.ai_advent_challenge_app.support

data class SupportUser(
    val id: String,
    val name: String,
    val email: String,
    val plan: String,
    val status: String,
    val locale: String,
    val lastLoginAt: String?,
    val failedLoginAttempts: Int,
    val twoFactorEnabled: Boolean
)

data class SupportTicket(
    val id: String,
    val userId: String,
    val subject: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdAt: String,
    val tags: List<String>,
    val events: List<SupportTicketEvent>
)

data class SupportTicketEvent(
    val timestamp: String,
    val type: String,
    val details: String
)

data class SupportCrmData(
    val users: List<SupportUser>,
    val tickets: List<SupportTicket>
)

data class SupportTicketContext(
    val ticket: SupportTicket,
    val user: SupportUser
)

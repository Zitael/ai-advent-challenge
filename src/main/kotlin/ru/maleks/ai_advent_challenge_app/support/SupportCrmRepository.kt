package ru.maleks.ai_advent_challenge_app.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

class SupportCrmRepository(
    dataFile: Path,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    private val data: SupportCrmData

    init {
        val normalized = dataFile.toAbsolutePath().normalize()
        require(Files.isRegularFile(normalized)) {
            "Support CRM data file not found: $normalized"
        }
        data = Files.newBufferedReader(normalized).use { reader ->
            objectMapper.readValue(reader)
        }
    }

    fun findUser(userId: String): SupportUser? =
        data.users.firstOrNull { it.id == userId }

    fun findTicket(ticketId: String): SupportTicket? =
        data.tickets.firstOrNull { it.id == ticketId }

    fun findTicketContext(ticketId: String): SupportTicketContext? {
        val ticket = findTicket(ticketId) ?: return null
        val user = findUser(ticket.userId) ?: return null
        return SupportTicketContext(ticket = ticket, user = user)
    }

    fun tickets(): List<SupportTicket> = data.tickets
}

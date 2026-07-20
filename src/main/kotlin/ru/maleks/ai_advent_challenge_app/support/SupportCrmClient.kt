package ru.maleks.ai_advent_challenge_app.support

import ru.maleks.ai_advent_challenge_app.mcp.client.McpClient

class SupportCrmClient(
    private val mcpClient: McpClient
) {
    suspend fun ticketContext(ticketId: String): String =
        mcpClient.callTool(
            name = "crm_get_ticket_context",
            arguments = mapOf("ticketId" to ticketId)
        ).text

    suspend fun listTickets(): String =
        mcpClient.callTool(
            name = "crm_list_tickets",
            arguments = emptyMap()
        ).text
}

package ru.maleks.ai_advent_challenge_app.mcp.support

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.maleks.ai_advent_challenge_app.support.SupportCrmRepository

class SupportMcpServerFactory(
    private val crmRepository: SupportCrmRepository
) {
    fun create(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "ai-advent-support-crm-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )

        registerGetTicket(server)
        registerGetUser(server)
        registerGetTicketContext(server)
        registerListTickets(server)
        return server
    }

    private fun registerGetTicket(server: Server) {
        server.addTool(
            name = "crm_get_ticket",
            description = "Return a support ticket by ticket id.",
            inputSchema = stringPropertySchema("ticketId", "Support ticket id")
        ) { request ->
            val ticketId = request.arguments?.get("ticketId")?.jsonPrimitive?.content.orEmpty()
            val ticket = crmRepository.findTicket(ticketId)
            textResult(ticket?.toString() ?: "Ticket not found: $ticketId")
        }
    }

    private fun registerGetUser(server: Server) {
        server.addTool(
            name = "crm_get_user",
            description = "Return a support user by user id.",
            inputSchema = stringPropertySchema("userId", "Support user id")
        ) { request ->
            val userId = request.arguments?.get("userId")?.jsonPrimitive?.content.orEmpty()
            val user = crmRepository.findUser(userId)
            textResult(user?.toString() ?: "User not found: $userId")
        }
    }

    private fun registerGetTicketContext(server: Server) {
        server.addTool(
            name = "crm_get_ticket_context",
            description = "Return a support ticket together with its user profile and event history.",
            inputSchema = stringPropertySchema("ticketId", "Support ticket id")
        ) { request ->
            val ticketId = request.arguments?.get("ticketId")?.jsonPrimitive?.content.orEmpty()
            val context = crmRepository.findTicketContext(ticketId)
            textResult(context?.toString() ?: "Ticket context not found: $ticketId")
        }
    }

    private fun registerListTickets(server: Server) {
        server.addTool(
            name = "crm_list_tickets",
            description = "Return available support tickets for demo purposes.",
            inputSchema = ToolSchema(properties = buildJsonObject { }, required = emptyList())
        ) {
            val text = crmRepository.tickets().joinToString("\n") { ticket ->
                "${ticket.id}: ${ticket.subject} [${ticket.status}, ${ticket.priority}]"
            }
            textResult(text.ifBlank { "No tickets found." })
        }
    }

    private fun stringPropertySchema(name: String, description: String): ToolSchema =
        ToolSchema(
            properties = buildJsonObject {
                put(name, buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive(description))
                })
            },
            required = listOf(name)
        )

    private fun textResult(text: String): CallToolResult =
        CallToolResult(content = listOf(TextContent(text = text)))
}

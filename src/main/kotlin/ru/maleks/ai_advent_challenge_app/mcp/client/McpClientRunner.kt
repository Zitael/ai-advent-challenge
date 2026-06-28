package ru.maleks.ai_advent_challenge_app.mcp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation

class McpClientRunner(
    private val config: McpServerConfig
) {

    suspend fun listTools(): List<McpToolInfo> {
        val httpClient = HttpClient(CIO) {
            install(SSE)
        }

        val client = Client(
            clientInfo = Implementation(
                name = config.clientName,
                version = config.clientVersion
            )
        )

        val transport = StreamableHttpClientTransport(
            client = httpClient,
            url = config.url
        )

        return try {
            client.connect(transport)

            client.listTools().tools.map { tool ->
                McpToolInfo(
                    name = tool.name,
                    description = tool.description ?: "",
                    inputSchema = tool.inputSchema.toString()
                )
            }
        } finally {
            httpClient.close()
        }
    }
}
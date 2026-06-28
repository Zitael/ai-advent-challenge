package ru.maleks.ai_advent_challenge_app.mcp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

class RemoteMcpClient(
    private val config: McpServerConfig
) : McpClient {

    override suspend fun listTools(): List<McpToolInfo> {
        return withConnectedClient { client ->
            client.listTools()
                .tools
                .map { tool ->
                    McpToolInfo(
                        name = tool.name,
                        description = tool.description ?: "",
                        inputSchema = tool.inputSchema.toString()
                    )
                }
        }
    }

    override suspend fun callTool(
        name: String,
        arguments: Map<String, Any?>
    ): McpToolCallResult {
        return withConnectedClient { client ->
            val result = client.callTool(
                name = name,
                arguments = arguments
            )

            val text = result.content.joinToString("\n") { content ->
                when (content) {
                    is TextContent -> content.text
                    else -> content.toString()
                }
            }

            McpToolCallResult(
                toolName = name,
                text = text
            )
        }
    }

    private suspend fun <T> withConnectedClient(block: suspend (Client) -> T): T {
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
            block(client)
        } finally {
            client.close()
            httpClient.close()
        }
    }
}
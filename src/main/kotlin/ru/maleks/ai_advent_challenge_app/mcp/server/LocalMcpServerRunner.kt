package ru.maleks.ai_advent_challenge_app.mcp.server

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp

class LocalMcpServerRunner(
    private val config: LocalMcpServerConfig = LocalMcpServerConfig(),
    private val factory: LocalMcpServerFactory = LocalMcpServerFactory()
) {
    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start() {
        if (engine != null) {
            return
        }

        val mcpServer = factory.create()

        engine = embeddedServer(
            factory = CIO,
            host = config.host,
            port = config.port
        ) {
            mcpStreamableHttp {
                mcpServer
            }
        }.start(wait = false)

        println("Local MCP server started: ${config.url}")
    }

    fun stop() {
        engine?.stop(
            gracePeriodMillis = 500,
            timeoutMillis = 1_000
        )
        engine = null
    }

    fun url(): String {
        return config.url
    }
}
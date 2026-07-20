package ru.maleks.ai_advent_challenge_app.mcp.support

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp

class SupportMcpServerRunner(
    private val host: String,
    private val port: Int,
    private val path: String,
    private val factory: SupportMcpServerFactory
) {
    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    val url: String
        get() = "http://$host:$port$path"

    fun start() {
        if (engine != null) return
        val server = factory.create()
        engine = embeddedServer(CIO, host = host, port = port) {
            mcpStreamableHttp { server }
        }.start(wait = false)
        println("SUPPORT CRM MCP server started: $url")
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 500, timeoutMillis = 1_000)
        engine = null
    }
}

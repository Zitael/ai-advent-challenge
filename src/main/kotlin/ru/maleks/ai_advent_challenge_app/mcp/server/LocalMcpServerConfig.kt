package ru.maleks.ai_advent_challenge_app.mcp.server

data class LocalMcpServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 3000,
    val path: String = "/mcp"
) {
    val url: String
        get() = "http://$host:$port$path"
}
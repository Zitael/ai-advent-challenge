package ru.maleks.ai_advent_challenge_app.mcp.server

data class LocalMcpServerConfig(
    val kind: LocalMcpServerKind,
    val host: String = "127.0.0.1",
    val port: Int,
    val path: String = "/mcp"
) {
    val url: String
        get() = "http://$host:$port$path"
}
package ru.maleks.ai_advent_challenge_app.gateway

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.nio.file.Path

fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val config = LlmGatewayConfig.from(dotenv)

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    val auditLogger = GatewayAuditLogger(Path.of(config.auditLogPath))
    val service = LlmGatewayService(
        config = config,
        proxyClient = OpenRouterProxyClient(
            httpClient = httpClient,
            apiKey = config.upstreamApiKey
        ),
        auditLogger = auditLogger
    )

    embeddedServer(ServerCIO, host = config.host, port = config.port) {
        module(config, service)
    }.start(wait = true)
}

fun Application.module(
    config: LlmGatewayConfig,
    service: LlmGatewayService
) {
    install(ServerContentNegotiation) { jackson() }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "internal error"))
            )
        }
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "llm-gateway"))
        }

        get("/metrics") {
            val snapshot = service.costSnapshot()
            call.respond(
                mapOf(
                    "totalRequests" to snapshot.totalRequests,
                    "totalPromptTokens" to snapshot.totalPromptTokens,
                    "totalCompletionTokens" to snapshot.totalCompletionTokens,
                    "totalCostUsd" to snapshot.totalCostUsd
                )
            )
        }

        post("/api/gateway/chat") {
            if (!isAuthorized(call.request.headers[HttpHeaders.Authorization], config.gatewayApiKey)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                return@post
            }

            val request = call.receive<GatewayChatRequest>()
            val clientIp = call.request.local.remoteHost

            when (val result = service.chat(clientIp, request)) {
                is GatewayServiceResult.RateLimited -> call.respond(
                    HttpStatusCode.TooManyRequests,
                    mapOf(
                        "error" to "rate_limit_exceeded",
                        "retryAfterSeconds" to result.retryAfterSeconds
                    )
                )

                is GatewayServiceResult.InputBlocked -> call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "input_guard_blocked",
                        "warning" to result.input.warning,
                        "findings" to result.input.findings.map { it.type.label }
                    )
                )

                is GatewayServiceResult.Success -> call.respond(result.response)
            }
        }
    }
}

private fun isAuthorized(header: String?, expected: String?): Boolean {
    if (expected.isNullOrBlank()) {
        return true
    }

    val token = header?.removePrefix("Bearer ")?.trim()
    return token == expected
}

package ru.maleks.ai_advent_challenge_app.battle

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
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
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import ru.maleks.ai_advent_challenge_app.gateway.GatewayAuditLogger
import ru.maleks.ai_advent_challenge_app.gateway.GatewayChatRequest
import ru.maleks.ai_advent_challenge_app.gateway.GatewayServiceResult
import ru.maleks.ai_advent_challenge_app.gateway.LlmGatewayConfig
import ru.maleks.ai_advent_challenge_app.gateway.LlmGatewayService
import ru.maleks.ai_advent_challenge_app.gateway.OpenRouterProxyClient
import ru.maleks.ai_advent_challenge_app.privateai.api.ErrorResponse
import ru.maleks.ai_advent_challenge_app.privateai.security.ApiKeyValidator
import ru.maleks.ai_advent_challenge_app.privateai.session.ChatSessionStore
import java.nio.file.Path

fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val config = BattlePipelineConfig.from(dotenv)

    val gatewayConfig = LlmGatewayConfig(
        host = config.host,
        port = config.port,
        upstreamApiKey = config.upstreamApiKey,
        defaultModel = config.defaultModel,
        gatewayApiKey = config.apiKey,
        rateLimitPerMinute = config.rateLimitPerMinute,
        auditLogPath = config.auditLogPath,
        defaultInputGuardMode = ru.maleks.ai_advent_challenge_app.gateway.InputGuardMode.BLOCK
    )

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    val auditLogger = GatewayAuditLogger(Path.of(config.auditLogPath))
    val gatewayService = LlmGatewayService(
        config = gatewayConfig,
        proxyClient = OpenRouterProxyClient(
            httpClient = httpClient,
            apiKey = config.upstreamApiKey
        ),
        auditLogger = auditLogger
    )

    val workspaceService = BattleWorkspaceService(Path.of(config.workspaceDirectory))

    val chatService = BattleChatService(
        config = config,
        gatewayService = gatewayService,
        pipelineGuard = BattlePipelineGuard(),
        sessionStore = ChatSessionStore(maxHistoryMessages = config.maxHistoryMessages),
        workspaceService = workspaceService,
        secretLeakGuard = BattleSecretLeakGuard(workspaceService)
    )

    val apiKeyValidator = ApiKeyValidator(config.apiKey)

    embeddedServer(ServerCIO, host = config.host, port = config.port) {
        battleModule(config, gatewayService, chatService, workspaceService, apiKeyValidator)
    }.start(wait = true)
}

fun Application.battleModule(
    config: BattlePipelineConfig,
    gatewayService: LlmGatewayService,
    chatService: BattleChatService,
    workspaceService: BattleWorkspaceService,
    apiKeyValidator: ApiKeyValidator
) {
    install(ServerContentNegotiation) { jackson() }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(status = 400, error = "bad_request", message = cause.message ?: "bad request")
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(status = 500, error = "internal_error", message = cause.message ?: "internal error")
            )
        }
    }

    routing {
        get("/") {
            val html = loadBattleChatHtml()
            call.respondBytes(html, ContentType.Text.Html)
        }

        get("/health") {
            call.respond(
                BattleHealthResponse(
                    status = "ok",
                    service = "battle-pipeline",
                    model = config.defaultModel,
                    layers = listOf(
                        "prompt-injection-guard",
                        "indirect-content-sanitizer",
                        "gateway-input-guard",
                        "gateway-output-guard",
                        "hardened-system-prompt",
                        "workspace-secret-leak-guard",
                        "security-review-execution-loop"
                    )
                )
            )
        }

        get("/metrics") {
            val snapshot = gatewayService.costSnapshot()
            call.respond(
                mapOf(
                    "totalRequests" to snapshot.totalRequests,
                    "totalPromptTokens" to snapshot.totalPromptTokens,
                    "totalCompletionTokens" to snapshot.totalCompletionTokens,
                    "totalCostUsd" to snapshot.totalCostUsd
                )
            )
        }

        get("/api/files") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@get
            }
            call.respond(workspaceService.listFiles())
        }

        get("/api/files/{fileName}") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@get
            }

            val fileName = call.parameters["fileName"].orEmpty()
            call.respond(workspaceService.readFile(fileName))
        }

        put("/api/files/{fileName}") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@put
            }

            val fileName = call.parameters["fileName"].orEmpty()
            val request = call.receive<BattleFileUploadRequest>()
            call.respond(workspaceService.saveFile(fileName, request.content))
        }

        delete("/api/files/{fileName}") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@delete
            }

            val fileName = call.parameters["fileName"].orEmpty()
            val deleted = workspaceService.deleteFile(fileName)
            call.respond(mapOf("deleted" to deleted, "fileName" to fileName))
        }

        post("/api/chat") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@post
            }

            val request = call.receive<BattleChatRequest>()
            val clientIp = call.request.local.remoteHost

            when (val result = chatService.chat(clientIp, request)) {
                is BattleChatResult.Success -> call.respond(result.response)
                is BattleChatResult.Blocked -> call.respond(
                    HttpStatusCode.OK,
                    BattleChatResponse(
                        sessionId = result.sessionId,
                        answer = result.answer,
                        model = config.defaultModel,
                        durationMillis = result.durationMillis,
                        historyMessages = 0,
                        guards = result.guards
                    )
                )
                is BattleChatResult.Error -> call.respond(
                    HttpStatusCode.TooManyRequests,
                    ErrorResponse(status = 429, error = "rate_limited", message = result.message)
                )
            }
        }

        delete("/api/sessions/{sessionId}") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@delete
            }

            val sessionId = call.parameters["sessionId"].orEmpty()
            val removed = chatService.clearSession(sessionId)
            call.respond(mapOf("message" to "Session cleared", "removedMessages" to removed))
        }

        post("/api/gateway/chat") {
            if (!apiKeyValidator.isValid(call.request.headers[HttpHeaders.Authorization])) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(status = 401, error = "unauthorized", message = "unauthorized")
                )
                return@post
            }

            val request = call.receive<GatewayChatRequest>()
            val clientIp = call.request.local.remoteHost

            when (val result = gatewayService.chat(clientIp, request)) {
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

private fun loadBattleChatHtml(): ByteArray {
    val stream = BattlePipelineServer::class.java.classLoader
        .getResourceAsStream("static/battle-chat.html")
        ?: error("static/battle-chat.html not found")

    return stream.use { it.readBytes() }
}

object BattlePipelineServer

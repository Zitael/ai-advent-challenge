package ru.maleks.ai_advent_challenge_app.privateai

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.CancellationException
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.privateai.api.ChatRequest
import ru.maleks.ai_advent_challenge_app.privateai.api.ErrorResponse
import ru.maleks.ai_advent_challenge_app.privateai.api.HealthResponse
import ru.maleks.ai_advent_challenge_app.privateai.api.SessionResponse
import ru.maleks.ai_advent_challenge_app.privateai.security.ApiKeyValidator
import ru.maleks.ai_advent_challenge_app.privateai.security.InMemoryRateLimiter
import ru.maleks.ai_advent_challenge_app.privateai.security.RateLimitDecision
import ru.maleks.ai_advent_challenge_app.privateai.service.OllamaHealthService
import ru.maleks.ai_advent_challenge_app.privateai.service.PrivateChatService
import ru.maleks.ai_advent_challenge_app.privateai.session.ChatSessionStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val config = PrivateAiConfig.from(dotenv)

    val ollamaHttpClient = createOllamaHttpClient()

    val ollamaClient = OllamaClient(
        httpClient = ollamaHttpClient,
        baseUrl = config.ollamaBaseUrl,
        model = config.ollamaModel
    )

    val sessionStore = ChatSessionStore(
        maxHistoryMessages = config.maxHistoryMessages
    )

    val chatService = PrivateChatService(
        config = config,
        ollamaClient = ollamaClient,
        sessionStore = sessionStore
    )

    val healthService = OllamaHealthService(
        httpClient = ollamaHttpClient,
        ollamaBaseUrl = config.ollamaBaseUrl
    )

    val apiKeyValidator = ApiKeyValidator(
        expectedApiKey = config.apiKey
    )

    val rateLimiter = InMemoryRateLimiter(
        maxRequests = config.rateLimitRequests,
        windowSeconds = config.rateLimitWindowSeconds
    )

    val server = embeddedServer(
        factory = ServerCIO,
        host = config.host,
        port = config.port
    ) {
        privateAiModule(
            config = config,
            chatService = chatService,
            healthService = healthService,
            apiKeyValidator = apiKeyValidator,
            rateLimiter = rateLimiter
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            serverLog(
                component = "SERVER",
                message = "Shutdown requested"
            )

            ollamaHttpClient.close()
        }
    )

    println("========================================")
    println("Private AI service")
    println("========================================")
    println("Web UI: http://localhost:${config.port}/")
    println("Host: ${config.host}")
    println("Port: ${config.port}")
    println("Model: ${config.ollamaModel}")
    println("Ollama: ${config.ollamaBaseUrl}")
    println(
        "Rate limit: ${config.rateLimitRequests} requests / " +
                "${config.rateLimitWindowSeconds} seconds"
    )
    println("Max message chars: ${config.maxMessageChars}")
    println("Max history messages: ${config.maxHistoryMessages}")
    println(
        "Max concurrent generations: " +
                config.maxConcurrentGenerations
    )
    println("Context window: ${config.contextWindow}")
    println("========================================")

    serverLog(
        component = "SERVER",
        message = "Starting HTTP server"
    )

    server.start(wait = true)
}

private fun createOllamaHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(ClientContentNegotiation) {
            jackson()
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }

        expectSuccess = true
    }
}

private fun Application.privateAiModule(
    config: PrivateAiConfig,
    chatService: PrivateChatService,
    healthService: OllamaHealthService,
    apiKeyValidator: ApiKeyValidator,
    rateLimiter: InMemoryRateLimiter
) {
    install(ServerContentNegotiation) {
        jackson()
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            serverLog(
                component = "ERROR",
                message =
                    "${call.request.httpMethod.value} " +
                            "${call.request.path()} -> 400: " +
                            "${cause.message}"
            )

            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorResponse(
                    status = HttpStatusCode.BadRequest.value,
                    error = "Bad Request",
                    message = cause.message ?: "Invalid request"
                )
            )
        }

        exception<CancellationException> { _, cause ->
            serverLog(
                component = "ERROR",
                message =
                    "Request coroutine was cancelled: " +
                            "${cause.message}"
            )

            throw cause
        }

        exception<Throwable> { call, cause ->
            serverLog(
                component = "ERROR",
                message =
                    "${call.request.httpMethod.value} " +
                            "${call.request.path()} -> 500: " +
                            "${cause::class.simpleName}: " +
                            "${cause.message}"
            )

            cause.printStackTrace()

            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    status =
                        HttpStatusCode.InternalServerError.value,
                    error = "Internal Server Error",
                    message = "The request could not be completed"
                )
            )
        }
    }

    routing {
        get("/") {
            executeLogged(call) {
                val html = loadIndexHtml()

                call.respondBytes(
                    bytes = html,
                    contentType = ContentType.Text.Html
                )
            }
        }

        get("/health") {
            executeLogged(call) {
                serverLog(
                    component = "HEALTH",
                    message = "Checking Ollama availability"
                )

                val ollamaAvailable =
                    healthService.isAvailable()

                serverLog(
                    component = "HEALTH",
                    message =
                        "Ollama status: " +
                                if (ollamaAvailable) {
                                    "AVAILABLE"
                                } else {
                                    "UNAVAILABLE"
                                }
                )

                call.respond(
                    status = if (ollamaAvailable) {
                        HttpStatusCode.OK
                    } else {
                        HttpStatusCode.ServiceUnavailable
                    },
                    message = HealthResponse(
                        status = if (ollamaAvailable) {
                            "UP"
                        } else {
                            "DEGRADED"
                        },
                        service = "private-ai",
                        ollama = if (ollamaAvailable) {
                            "AVAILABLE"
                        } else {
                            "UNAVAILABLE"
                        },
                        model = config.ollamaModel
                    )
                )
            }
        }

        route("/api") {
            post("/chat") {
                executeLogged(call) {
                    serverLog(
                        component = "AUTH",
                        message = "Validating API key"
                    )

                    if (!authorize(call, apiKeyValidator)) {
                        serverLog(
                            component = "AUTH",
                            message = "API key rejected"
                        )

                        return@executeLogged
                    }

                    serverLog(
                        component = "AUTH",
                        message = "API key accepted"
                    )

                    val clientId = resolveClientId(call)

                    serverLog(
                        component = "RATE_LIMIT",
                        message = "Client ID: $clientId"
                    )

                    when (
                        val decision =
                            rateLimiter.tryAcquire(clientId)
                    ) {
                        is RateLimitDecision.Denied -> {
                            serverLog(
                                component = "RATE_LIMIT",
                                message =
                                    "Request denied. Retry after " +
                                            "${decision.retryAfterSeconds} seconds"
                            )

                            call.response.header(
                                HttpHeaders.RetryAfter,
                                decision.retryAfterSeconds.toString()
                            )

                            call.respond(
                                status =
                                    HttpStatusCode.TooManyRequests,
                                message = ErrorResponse(
                                    status =
                                        HttpStatusCode
                                            .TooManyRequests
                                            .value,
                                    error = "Too Many Requests",
                                    message =
                                        "Rate limit exceeded. " +
                                                "Retry after " +
                                                "${decision.retryAfterSeconds} " +
                                                "seconds."
                                )
                            )

                            return@executeLogged
                        }

                        is RateLimitDecision.Allowed -> {
                            serverLog(
                                component = "RATE_LIMIT",
                                message =
                                    "Request allowed. Remaining: " +
                                            decision.remainingRequests
                            )

                            call.response.header(
                                "X-RateLimit-Remaining",
                                decision
                                    .remainingRequests
                                    .toString()
                            )
                        }
                    }

                    serverLog(
                        component = "HTTP",
                        message = "Reading chat request body"
                    )

                    val request =
                        call.receive<ChatRequest>()

                    serverLog(
                        component = "HTTP",
                        message =
                            "Chat request parsed. Session: " +
                                    request.sessionId
                    )

                    val response =
                        chatService.chat(request)

                    serverLog(
                        component = "HTTP",
                        message =
                            "Sending chat response. " +
                                    "Duration: ${response.durationMillis} ms"
                    )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = response
                    )
                }
            }

            get("/sessions/{sessionId}") {
                executeLogged(call) {
                    if (!authorize(call, apiKeyValidator)) {
                        return@executeLogged
                    }

                    val sessionId =
                        call.parameters["sessionId"]
                            ?: throw IllegalArgumentException(
                                "sessionId is required"
                            )

                    val historyMessages =
                        chatService.historySize(sessionId)

                    serverLog(
                        component = "SESSION",
                        message =
                            "Session $sessionId contains " +
                                    "$historyMessages messages"
                    )

                    call.respond(
                        SessionResponse(
                            sessionId = sessionId,
                            historyMessages = historyMessages,
                            message = "Session found"
                        )
                    )
                }
            }

            delete("/sessions/{sessionId}") {
                executeLogged(call) {
                    if (!authorize(call, apiKeyValidator)) {
                        return@executeLogged
                    }

                    val sessionId =
                        call.parameters["sessionId"]
                            ?: throw IllegalArgumentException(
                                "sessionId is required"
                            )

                    val removedMessages =
                        chatService.clearSession(sessionId)

                    call.respond(
                        SessionResponse(
                            sessionId = sessionId,
                            historyMessages = 0,
                            message =
                                "Session cleared. " +
                                        "Removed messages: " +
                                        removedMessages
                        )
                    )
                }
            }
        }
    }
}

private suspend fun executeLogged(
    call: ApplicationCall,
    block: suspend () -> Unit
) {
    val startedAt = System.currentTimeMillis()
    val method = call.request.httpMethod.value
    val path = call.request.path()

    serverLog(
        component = "HTTP",
        message = "-> $method $path"
    )

    try {
        block()
    } finally {
        val durationMillis =
            System.currentTimeMillis() - startedAt

        serverLog(
            component = "HTTP",
            message =
                "<- $method $path completed in " +
                        "$durationMillis ms"
        )
    }
}

private fun loadIndexHtml(): ByteArray {
    val resourcePath = "/static/index.html"

    return object {}
        .javaClass
        .getResourceAsStream(resourcePath)
        ?.use { input ->
            input.readBytes()
        }
        ?: error(
            "Web UI resource was not found: $resourcePath"
        )
}

private suspend fun authorize(
    call: ApplicationCall,
    apiKeyValidator: ApiKeyValidator
): Boolean {
    val authorizationHeader =
        call.request.headers[HttpHeaders.Authorization]

    if (apiKeyValidator.isValid(authorizationHeader)) {
        return true
    }

    call.respond(
        status = HttpStatusCode.Unauthorized,
        message = ErrorResponse(
            status = HttpStatusCode.Unauthorized.value,
            error = "Unauthorized",
            message = "Valid Bearer API key is required"
        )
    )

    return false
}

private fun resolveClientId(
    call: ApplicationCall
): String {
    return call.request.headers["X-Forwarded-For"]
        ?.substringBefore(",")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "direct-client"
}

private fun serverLog(
    component: String,
    message: String
) {
    println(
        "[${currentTimestamp()}] [$component] $message"
    )
}

private fun currentTimestamp(): String {
    return LocalDateTime.now().format(LOG_TIME_FORMAT)
}

private val LOG_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
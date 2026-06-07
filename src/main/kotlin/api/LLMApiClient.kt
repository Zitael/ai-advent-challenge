package api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class LLMApiClient {

    suspend fun send(
        client: HttpClient,
        req: OpenRouterRequest,
        apiKey: String
    ): String {
        val response: OpenRouterResponse = client.post("https://openrouter.ai/api/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

        return response
            .choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"
    }
}
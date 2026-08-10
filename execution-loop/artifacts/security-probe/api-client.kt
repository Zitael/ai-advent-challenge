package probe

import io.ktor.client.*
import io.ktor.client.request.*

class ApiClient(private val client: HttpClient) {
    suspend fun fetchUser(userInput: String): String {
        val url = "http://api.example.com/users/" + userInput
        return client.get(url).bodyAsText()
    }
}
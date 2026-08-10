package probe

class RequestLogger {
    fun logRequest(url: String, headers: Map<String, String>, body: String) {
        println("REQUEST url=$url headers=$headers body=$body")
        println("Authorization header: ${headers["Authorization"]}")
    }
}
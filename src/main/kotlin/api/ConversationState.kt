package api

data class ConversationState(
    var summary: String = "",
    var messages: MutableList<OpenRouterMessage> = mutableListOf()
)
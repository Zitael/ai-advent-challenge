package ru.ai_advent_app.day1.api

import api.OpenRouterMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class ConversationMemory(
    private val filePath: String = "conversation-history.json"
) {
    private val objectMapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun load(): MutableList<OpenRouterMessage> {
        if (!file.exists()) {
            return mutableListOf()
        }

        return objectMapper.readValue(file)
    }

    fun save(messages: List<OpenRouterMessage>) {
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, messages)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
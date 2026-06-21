package ru.maleks.ai_advent_challenge_app.archive.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class ConversationMemory(
    private val filePath: String = "conversation-state.json"
) {
    private val objectMapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun load(): ConversationState {
        if (!file.exists()) {
            return ConversationState()
        }

        return objectMapper.readValue(file)
    }

    fun save(state: ConversationState) {
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, state)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
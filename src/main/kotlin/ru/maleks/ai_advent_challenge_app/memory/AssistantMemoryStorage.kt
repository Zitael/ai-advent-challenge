package ru.maleks.ai_advent_challenge_app.memory

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class AssistantMemoryStorage(
    private val filePath: String = "assistant-memory.json"
) {
    private val mapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun load(): AssistantMemory {
        if (!file.exists()) {
            return AssistantMemory()
        }

        return mapper.readValue(file)
    }

    fun save(memory: AssistantMemory) {
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, memory)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
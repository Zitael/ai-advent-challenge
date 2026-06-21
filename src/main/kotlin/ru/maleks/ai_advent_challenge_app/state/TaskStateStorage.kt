package ru.maleks.ai_advent_challenge_app.state

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class TaskStateStorage(
    private val filePath: String = "task-state.json"
) {
    private val mapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun load(): TaskState {
        if (!file.exists()) {
            return TaskState()
        }

        return mapper.readValue(file)
    }

    fun save(state: TaskState) {
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, state)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
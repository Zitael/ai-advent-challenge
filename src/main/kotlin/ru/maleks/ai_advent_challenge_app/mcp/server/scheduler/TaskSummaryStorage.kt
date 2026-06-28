package ru.maleks.ai_advent_challenge_app.mcp.server.scheduler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class TaskSummaryStorage(
    private val filePath: String = "task-summary.json"
) {
    private val mapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun load(): TaskSummaryState {
        if (!file.exists()) {
            return TaskSummaryState()
        }

        return mapper.readValue(file)
    }

    fun save(state: TaskSummaryState) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, state)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
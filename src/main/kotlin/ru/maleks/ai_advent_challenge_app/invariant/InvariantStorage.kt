package ru.maleks.ai_advent_challenge_app.invariant

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

class InvariantStorage(
    private val filePath: String = "invariants.json"
) {
    private val mapper = jacksonObjectMapper()
    private val file = File(filePath)

    fun load(): MutableList<Invariant> {
        if (!file.exists()) {
            return mutableListOf()
        }

        return mapper.readValue(file)
    }

    fun save(invariants: List<Invariant>) {
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(file, invariants)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
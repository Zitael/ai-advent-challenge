package ru.maleks.ai_advent_challenge_app.rag.index

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ru.maleks.ai_advent_challenge_app.rag.model.DocumentIndex
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DocumentIndexStorage {

    private val mapper = jacksonObjectMapper()

    fun save(index: DocumentIndex, path: Path) {
        Files.createDirectories(path.parent)
        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(index)
        path.writeText(json)
    }

    fun load(path: Path): DocumentIndex {
        return mapper.readValue(path.readText())
    }
}
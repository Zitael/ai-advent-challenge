package ru.maleks.ai_advent_challenge_app.rag.chat

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class RagChatStorage(
    private val path: Path = Path.of("rag-index", "rag-chat-state.json")
) {
    private val mapper = jacksonObjectMapper()

    fun load(): RagChatState {
        if (!Files.exists(path)) {
            return RagChatState()
        }

        return mapper.readValue(path.readText(Charsets.UTF_8))
    }

    fun save(state: RagChatState) {
        Files.createDirectories(path.parent)
        path.writeText(
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(state),
            Charsets.UTF_8
        )
    }

    fun clear() {
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }
}
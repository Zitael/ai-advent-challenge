package ru.maleks.ai_advent_challenge_app.dataset

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class JsonlDatasetIO {
    private val mapper = jacksonObjectMapper()

    fun write(path: Path, examples: List<FineTuningExample>) {
        Files.createDirectories(path.parent)
        val content = examples.joinToString(separator = "\n") { example ->
            mapper.writeValueAsString(mapOf("messages" to example.messages))
        }
        Files.writeString(path, content + "\n", StandardCharsets.UTF_8)
    }

    fun read(path: Path): List<FineTuningExample> {
        if (!Files.exists(path)) {
            return emptyList()
        }

        return Files.readAllLines(path, StandardCharsets.UTF_8)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val node = mapper.readTree(line)
                val messagesNode = node.get("messages")
                    ?: error("Missing messages field")
                val messages = mapper.readerForListOf(FineTuningMessage::class.java)
                    .readValue<List<FineTuningMessage>>(messagesNode)
                FineTuningExample(messages = messages)
            }
            .toList()
    }
}

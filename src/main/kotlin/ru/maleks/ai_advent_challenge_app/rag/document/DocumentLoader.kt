package ru.maleks.ai_advent_challenge_app.rag.document

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.readText

class DocumentLoader(
    private val rootDirectory: Path
) {
    private val supportedExtensions = setOf("md", "txt", "kt", "java", "xml", "yml", "yaml")

    fun load(): List<RawDocument> {
        if (!Files.exists(rootDirectory)) {
            error("Knowledge directory does not exist: $rootDirectory")
        }

        return Files.walk(rootDirectory)
            .filter { Files.isRegularFile(it) }
            .filter { it.extension.lowercase() in supportedExtensions }
            .map { path ->
                val text = path.readText()
                RawDocument(
                    source = path.relativeTo(rootDirectory).toString(),
                    title = extractTitle(path, text),
                    text = text
                )
            }
            .toList()
    }

    private fun extractTitle(path: Path, text: String): String {
        val heading = text
            .lineSequence()
            .firstOrNull { it.trim().startsWith("# ") }
            ?.trim()
            ?.removePrefix("#")
            ?.trim()

        return heading ?: path.name
    }
}
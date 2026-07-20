package ru.maleks.ai_advent_challenge_app.support

import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import java.nio.file.Files
import java.nio.file.Path

class SupportDocumentLoader(
    private val projectRoot: Path
) {
    fun load(): List<RawDocument> {
        val directory = projectRoot.resolve("support-docs")
        if (!Files.isDirectory(directory)) return emptyList()

        return Files.walk(directory).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter { it.fileName.toString().endsWith(".md", ignoreCase = true) }
                .sorted()
                .map { file ->
                    RawDocument(
                        source = projectRoot.relativize(file).toString().replace('\\', '/'),
                        title = file.fileName.toString().removeSuffix(".md"),
                        text = Files.readString(file)
                    )
                }
                .toList()
        }
    }
}

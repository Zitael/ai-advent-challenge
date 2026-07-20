package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo

class ProjectDocumentLoader(
    private val projectRoot: Path
) {
    private val supportedExtensions = setOf(
        "md", "txt", "adoc", "yaml", "yml", "json"
    )

    fun load(): List<RawDocument> {
        require(Files.isDirectory(projectRoot)) {
            "Project root does not exist: ${projectRoot.toAbsolutePath()}"
        }

        val files = buildList {
            val readme = projectRoot.resolve("README.md")
            if (Files.isRegularFile(readme)) {
                add(readme)
            }

            val docsDirectory = projectRoot.resolve("docs")
            if (Files.isDirectory(docsDirectory)) {
                Files.walk(docsDirectory).use { paths ->
                    paths
                        .filter { Files.isRegularFile(it) }
                        .filter { it.extension.lowercase() in supportedExtensions }
                        .forEach(::add)
                }
            }
        }.distinct()

        return files.map { path ->
            val text = path.readText()
            RawDocument(
                source = path.relativeTo(projectRoot).toString().replace('\\', '/'),
                title = extractTitle(path, text),
                text = text
            )
        }
    }

    private fun extractTitle(path: Path, text: String): String {
        val markdownTitle = text.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()

        return markdownTitle?.takeIf(String::isNotBlank) ?: path.name
    }
}

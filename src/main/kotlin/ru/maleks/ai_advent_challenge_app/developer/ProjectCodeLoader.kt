package ru.maleks.ai_advent_challenge_app.developer

import ru.maleks.ai_advent_challenge_app.rag.document.RawDocument
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo

class ProjectCodeLoader(
    private val projectRoot: Path
) {
    private val supportedExtensions = setOf(
        "kt", "kts", "java", "xml", "properties", "yaml", "yml", "json"
    )

    private val ignoredDirectories = setOf(
        ".git", ".gradle", ".idea", ".kotlin", "build", "out", "node_modules"
    )

    fun load(): List<RawDocument> {
        require(Files.isDirectory(projectRoot)) {
            "Project root does not exist: ${projectRoot.toAbsolutePath()}"
        }

        return Files.walk(projectRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .filter { it.extension.lowercase() in supportedExtensions }
                .filter { path ->
                    path.relativeTo(projectRoot).none { part ->
                        part.toString() in ignoredDirectories
                    }
                }
                .map { path ->
                    RawDocument(
                        source = path.relativeTo(projectRoot).toString().replace('\\', '/'),
                        title = path.name,
                        text = path.readText()
                    )
                }
                .toList()
        }
    }
}

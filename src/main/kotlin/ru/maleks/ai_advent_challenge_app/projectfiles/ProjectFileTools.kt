package ru.maleks.ai_advent_challenge_app.projectfiles

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class ProjectFileTools(
    projectRoot: Path
) {
    val root: Path = projectRoot.toAbsolutePath().normalize()

    fun searchableFiles(): List<Path> =
        Files.walk(root).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { isAllowed(it) }
                .filter { !isExcluded(it) }
                .sorted()
                .toList()
        }

    fun search(term: String, limit: Int = 200): List<FileMatch> {
        require(term.isNotBlank()) { "Search term must not be blank" }
        val result = mutableListOf<FileMatch>()
        for (file in searchableFiles()) {
            Files.readAllLines(file, StandardCharsets.UTF_8)
                .forEachIndexed { index, line ->
                    if (line.contains(term, ignoreCase = true)) {
                        result += FileMatch(root.relativize(file), index + 1, line.trim())
                    }
                }
            if (result.size >= limit) break
        }
        return result.take(limit)
    }

    fun read(relativePath: Path): String {
        val file = safeResolve(relativePath)
        require(Files.isRegularFile(file)) { "File not found: $relativePath" }
        return Files.readString(file, StandardCharsets.UTF_8)
    }

    fun write(relativePath: Path, content: String): FileChange {
        val file = safeResolve(relativePath)
        val before = if (Files.exists(file)) Files.readString(file, StandardCharsets.UTF_8) else null
        Files.createDirectories(file.parent)
        Files.writeString(file, content, StandardCharsets.UTF_8)
        return FileChange(relativePath, before, content)
    }

    fun relative(path: Path): Path = root.relativize(path.toAbsolutePath().normalize())

    private fun safeResolve(relativePath: Path): Path {
        val resolved = root.resolve(relativePath).normalize()
        require(resolved.startsWith(root)) { "Path is outside project: $relativePath" }
        return resolved
    }

    private fun isExcluded(path: Path): Boolean {
        val relative = root.relativize(path).toString().replace('\\', '/')
        return EXCLUDED_PREFIXES.any { relative.startsWith(it) }
    }

    private fun isAllowed(path: Path): Boolean {
        val name = path.fileName.toString().lowercase()
        return path.extension.lowercase() in ALLOWED_EXTENSIONS || name in ALLOWED_NAMES
    }

    private companion object {
        val EXCLUDED_PREFIXES = listOf(".git/", ".gradle/", ".idea/", "build/", ".kotlin/")
        val ALLOWED_EXTENSIONS = setOf("kt", "kts", "java", "md", "json", "yaml", "yml", "xml", "properties", "sql")
        val ALLOWED_NAMES = setOf("readme", "readme.md", "dockerfile", ".gitignore")
    }
}

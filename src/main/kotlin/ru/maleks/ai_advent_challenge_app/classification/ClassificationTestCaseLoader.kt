package ru.maleks.ai_advent_challenge_app.classification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class ClassificationTestCaseLoader {
    private val mapper = jacksonObjectMapper()

    fun load(path: Path): List<ClassificationTestCase> {
        require(path.isRegularFile()) {
            "Test cases file not found: $path"
        }

        val raw: List<RawTestCase> = Files.newBufferedReader(path).use { reader ->
            mapper.readValue(reader)
        }

        return raw.map { item ->
            ClassificationTestCase(
                id = item.id,
                ticketText = item.ticketText,
                expectedCategory = item.expectedCategory,
                kind = TestCaseKind.valueOf(item.kind.uppercase())
            )
        }
    }

    private data class RawTestCase(
        val id: String,
        val ticketText: String,
        val expectedCategory: String? = null,
        val kind: String
    )
}

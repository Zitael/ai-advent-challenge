package ru.maleks.ai_advent_challenge_app.decomposition

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class DecompositionTestCaseLoader {
    private val mapper = jacksonObjectMapper()

    fun load(path: Path): List<DecompositionTestCase> {
        require(path.isRegularFile()) {
            "Decomposition test cases file not found: $path"
        }

        val raw: List<RawDecompositionTestCase> = Files.newBufferedReader(path).use { reader ->
            mapper.readValue(reader)
        }

        return raw.map { item ->
            DecompositionTestCase(
                id = item.id,
                ticketText = item.ticketText,
                expectedCategory = item.expectedCategory,
                expectedPriority = item.expectedPriority,
                expectedAction = item.expectedAction
            )
        }
    }

    private data class RawDecompositionTestCase(
        val id: String,
        val ticketText: String,
        val expectedCategory: String? = null,
        val expectedPriority: String? = null,
        val expectedAction: String? = null
    )
}

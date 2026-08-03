package ru.maleks.ai_advent_challenge_app.micromodel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class MicromodelTestCaseLoader {
    private val mapper = jacksonObjectMapper()

    fun load(path: Path): List<MicromodelTestCase> {
        require(path.isRegularFile()) {
            "Micromodel test cases file not found: $path"
        }

        val raw: List<RawMicromodelTestCase> = Files.newBufferedReader(path).use { reader ->
            mapper.readValue(reader)
        }

        return raw.map { item ->
            MicromodelTestCase(
                id = item.id,
                ticketText = item.ticketText,
                expectedCategory = item.expectedCategory,
                kind = MicromodelTestKind.valueOf(item.kind.uppercase())
            )
        }
    }

    private data class RawMicromodelTestCase(
        val id: String,
        val ticketText: String,
        val expectedCategory: String? = null,
        val kind: String
    )
}

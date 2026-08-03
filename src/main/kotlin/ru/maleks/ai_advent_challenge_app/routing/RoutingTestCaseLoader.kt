package ru.maleks.ai_advent_challenge_app.routing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class RoutingTestCaseLoader {
    private val mapper = jacksonObjectMapper()

    fun load(path: Path): List<RoutingTestCase> {
        require(path.isRegularFile()) {
            "Routing test cases file not found: $path"
        }

        val raw: List<RawRoutingTestCase> = Files.newBufferedReader(path).use { reader ->
            mapper.readValue(reader)
        }

        return raw.map { item ->
            RoutingTestCase(
                id = item.id,
                ticketText = item.ticketText,
                expectedCategory = item.expectedCategory,
                kind = RoutingTestKind.valueOf(item.kind.uppercase()),
                expectEscalation = item.expectEscalation
            )
        }
    }

    private data class RawRoutingTestCase(
        val id: String,
        val ticketText: String,
        val expectedCategory: String? = null,
        val kind: String,
        val expectEscalation: Boolean? = null
    )
}

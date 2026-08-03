package ru.maleks.ai_advent_challenge_app.dataset

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class RealTicketSource(
    private val realTicketsFile: Path
) {
    private val mapper = jacksonObjectMapper()

    fun load(): List<FineTuningExample> {
        require(realTicketsFile.isRegularFile()) {
            "Real tickets file not found: $realTicketsFile"
        }

        val raw: List<RawTicketExample> = Files.newBufferedReader(realTicketsFile).use { reader ->
            mapper.readValue(reader)
        }

        return raw.mapNotNull { ticket ->
            val category = TicketCategory.fromLabel(ticket.category) ?: return@mapNotNull null
            toExample(
                userMessage = ticket.userMessage,
                category = category,
                source = ticket.source,
                real = true
            )
        }
    }

    companion object {
        fun toExample(
            userMessage: String,
            category: TicketCategory,
            source: String,
            real: Boolean
        ): FineTuningExample = FineTuningExample(
            messages = listOf(
                FineTuningMessage(role = "system", content = DatasetSystemPrompt.TICKET_CLASSIFIER),
                FineTuningMessage(role = "user", content = DatasetSystemPrompt.userContent(userMessage)),
                FineTuningMessage(role = "assistant", content = category.label)
            ),
            source = source,
            real = real
        )
    }
}

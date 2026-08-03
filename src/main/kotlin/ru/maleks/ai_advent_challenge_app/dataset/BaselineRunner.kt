package ru.maleks.ai_advent_challenge_app.dataset

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.maleks.ai_advent_challenge_app.llm.LlmClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class BaselineRunner(
    private val llmClient: LlmClient,
    private val model: String,
    private val jsonlIo: JsonlDatasetIO = JsonlDatasetIO(),
    private val sampleCount: Int = 10
) {
    private val mapper = jacksonObjectMapper()

    suspend fun run(
        evalPath: Path,
        outputPath: Path
    ): BaselineReport {
        val evalExamples = jsonlIo.read(evalPath)
        require(evalExamples.isNotEmpty()) {
            "Eval dataset is empty: $evalPath"
        }

        val samples = evalExamples.take(sampleCount)
        val results = mutableListOf<BaselineSampleResult>()

        samples.forEachIndexed { index, example ->
            val systemMessage = example.messages.first { it.role == "system" }
            val userMessage = example.messages.first { it.role == "user" }
            val expected = example.messages.first { it.role == "assistant" }.content.trim().lowercase()

            val response = llmClient.complete(
                messages = listOf(
                    OpenRouterMessage(role = systemMessage.role, content = systemMessage.content),
                    OpenRouterMessage(role = userMessage.role, content = userMessage.content)
                )
            ).answer.trim()

            val normalized = response.lowercase()
                .lineSequence()
                .first()
                .trim()
                .removeSuffix(".")
                .removeSuffix(",")

            results += BaselineSampleResult(
                index = index + 1,
                userMessage = userMessage.content,
                expectedCategory = expected,
                modelAnswer = response,
                exactMatch = normalized == expected,
                formatValid = TicketCategory.fromLabel(normalized) != null
            )
        }

        val report = BaselineReport(
            model = model,
            sampleCount = results.size,
            exactMatches = results.count { it.exactMatch },
            formatValidCount = results.count { it.formatValid },
            samples = results
        )

        Files.createDirectories(outputPath.parent)
        Files.writeString(
            outputPath,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardCharsets.UTF_8
        )

        return report
    }
}

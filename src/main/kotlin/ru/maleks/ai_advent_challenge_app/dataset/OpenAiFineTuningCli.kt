package ru.maleks.ai_advent_challenge_app.dataset

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.nio.file.Path

suspend fun main(args: Array<String>) {
    val execute = args.contains("--execute")
    val dotenv = dotenv { ignoreIfMissing = true }

    val apiKey = dotenv["OPENAI_API_KEY"]
        ?: System.getenv("OPENAI_API_KEY")

    val baseModel = dotenv["OPENAI_FINE_TUNE_MODEL"]
        ?: System.getenv("OPENAI_FINE_TUNE_MODEL")
        ?: "gpt-4o-mini-2024-07-18"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val trainPath = projectRoot.resolve("dataset/train.jsonl")

    System.out.println("AI Advent Challenge — Day 6")
    System.out.println("OpenAI Fine-Tuning Client")
    System.out.println()
    System.out.println("Training file: $trainPath")
    System.out.println("Base model: $baseModel")
    System.out.println("Mode: ${if (execute) "EXECUTE" else "DRY-RUN"}")
    System.out.println()

    if (!execute) {
        System.out.println("Prepared workflow:")
        System.out.println("1. upload file -> POST /v1/files (purpose=fine-tune)")
        System.out.println("2. create job -> POST /v1/fine_tuning/jobs")
        System.out.println("3. poll status -> GET /v1/fine_tuning/jobs/{id}")
        System.out.println()
        System.out.println("To run for real: add --execute and set OPENAI_API_KEY")
        return
    }

    require(!apiKey.isNullOrBlank()) {
        "OPENAI_API_KEY is required for --execute mode"
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val client = OpenAiFineTuningClient(
            httpClient = httpClient,
            apiKey = apiKey
        )

        System.out.println("Uploading training file...")
        val uploaded = client.uploadTrainingFile(trainPath)
        System.out.println("Uploaded file id: ${uploaded.id}")

        System.out.println("Creating fine-tuning job...")
        val job = client.createFineTuningJob(
            trainingFileId = uploaded.id,
            model = baseModel,
            suffix = "ticket-classifier"
        )
        System.out.println("Job id: ${job.id}, status: ${job.status}")

        System.out.println("Polling job status...")
        val finalJob = client.pollFineTuningJob(job.id) { current ->
            System.out.println("Status: ${current.status}")
        }

        System.out.println("Final status: ${finalJob.status}")
        finalJob.fineTunedModel?.let { System.out.println("Fine-tuned model: $it") }
        finalJob.error?.message?.let { System.out.println("Error: $it") }
    } finally {
        httpClient.close()
    }
}

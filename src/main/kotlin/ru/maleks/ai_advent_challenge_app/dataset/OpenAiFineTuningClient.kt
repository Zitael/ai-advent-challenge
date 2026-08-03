package ru.maleks.ai_advent_challenge_app.dataset

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.nio.file.Path
import kotlin.io.path.readBytes

class OpenAiFineTuningClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1"
) {
    suspend fun uploadTrainingFile(
        trainingFile: Path,
        purpose: String = "fine-tune"
    ): OpenAiFileObject {
        val response = httpClient.submitFormWithBinaryData(
            url = "$baseUrl/files",
            formData = formData {
                append("purpose", purpose)
                append(
                    key = "file",
                    value = trainingFile.readBytes(),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/jsonl")
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"${trainingFile.fileName}\""
                        )
                    }
                )
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }

        return response.body()
    }

    suspend fun createFineTuningJob(
        trainingFileId: String,
        model: String,
        suffix: String? = null
    ): OpenAiFineTuningJob {
        return httpClient.post("$baseUrl/fine_tuning/jobs") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                OpenAiCreateFineTuningJobRequest(
                    trainingFile = trainingFileId,
                    model = model,
                    suffix = suffix
                )
            )
        }.body()
    }

    suspend fun getFineTuningJob(jobId: String): OpenAiFineTuningJob {
        return httpClient.get("$baseUrl/fine_tuning/jobs/$jobId") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    suspend fun pollFineTuningJob(
        jobId: String,
        maxAttempts: Int = 60,
        delayMillis: Long = 5_000,
        onStatus: suspend (OpenAiFineTuningJob) -> Unit = {}
    ): OpenAiFineTuningJob {
        repeat(maxAttempts) { attempt ->
            val job = getFineTuningJob(jobId)
            onStatus(job)

            when (job.status.lowercase()) {
                "succeeded", "failed", "cancelled" -> return job
            }

            if (attempt < maxAttempts - 1) {
                kotlinx.coroutines.delay(delayMillis)
            }
        }

        return getFineTuningJob(jobId)
    }
}

data class OpenAiCreateFineTuningJobRequest(
    @JsonProperty("training_file")
    val trainingFile: String,
    val model: String,
    val suffix: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiFileObject(
    val id: String,
    val filename: String? = null,
    val purpose: String? = null,
    val status: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiFineTuningJob(
    val id: String,
    val status: String,
    val model: String? = null,
    @JsonProperty("fine_tuned_model")
    val fineTunedModel: String? = null,
    @JsonProperty("trained_tokens")
    val trainedTokens: Int? = null,
    val error: OpenAiFineTuningJobError? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiFineTuningJobError(
    val message: String? = null,
    val code: String? = null
)

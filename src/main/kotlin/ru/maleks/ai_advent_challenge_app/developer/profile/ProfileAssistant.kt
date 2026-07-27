package ru.maleks.ai_advent_challenge_app.developer.profile

import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaClient
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaGenerationConfig
import ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaOptions

class ProfileAssistant(
    private val ollamaClient: OllamaClient,
    private val contextCollector: ProfileContextCollector,
    private val promptBuilder: ProfilePromptBuilder = ProfilePromptBuilder()
) {
    suspend fun execute(profile: AssistantProfile, task: String): String {
        val context = contextCollector.collect(task, profile)
        val prompt = promptBuilder.build(profile, task, context)

        return ollamaClient.complete(
            prompt = prompt,
            config = PROFILE_GENERATION_CONFIG
        ).answer
    }

    private companion object {
        val PROFILE_GENERATION_CONFIG = OllamaGenerationConfig(
            name = "agent-profile",
            options = OllamaOptions(
                temperature = 0.15,
                num_predict = 2200,
                num_ctx = 16_384,
                top_p = 0.8,
                repeat_penalty = 1.12
            ),
            think = true
        )
    }
}

package ru.maleks.ai_advent_challenge_app.llm.ollama

object OllamaOptimizationProfiles {

    val baseline = OllamaGenerationConfig(
        name = "baseline",
        options = OllamaOptions(
            temperature = 0.7,
            num_predict = 700,
            num_ctx = 4096,
            top_p = 0.9,
            repeat_penalty = 1.1
        ),
        think = true
    )

    val optimizedRag = OllamaGenerationConfig(
        name = "optimized-rag",
        options = OllamaOptions(
            temperature = 0.2,
            num_predict = 350,
            num_ctx = 8192,
            top_p = 0.8,
            repeat_penalty = 1.15
        ),
        think = false
    )
}
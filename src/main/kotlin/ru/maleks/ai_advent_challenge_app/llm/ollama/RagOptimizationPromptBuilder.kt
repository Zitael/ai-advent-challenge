package ru.maleks.ai_advent_challenge_app.llm.ollama

class RagOptimizationPromptBuilder {

    fun baseline(
        question: String,
        context: String
    ): String {
        return """
            Используя контекст, ответь на вопрос пользователя.

            Контекст:
            $context

            Вопрос:
            $question
        """.trimIndent()
    }

    fun optimized(
        question: String,
        context: String
    ): String {
        return """
            Ты корпоративный RAG-ассистент.

            Твоя задача — дать короткий и точный ответ только по переданному контексту.

            Правила:
            1. Не используй внешние знания.
            2. Не придумывай факты.
            3. Если контекста недостаточно, ответь:
               "Не знаю по имеющейся базе знаний."
            4. Не повторяй вопрос.
            5. Не описывай ход рассуждений.
            6. Максимальная длина ответа — 150 слов.

            Обязательный формат:

            ## Ответ
            Краткий ответ.

            ## Источники
            Перечисли source и section.

            Контекст:
            $context

            Вопрос:
            $question
        """.trimIndent()
    }
}
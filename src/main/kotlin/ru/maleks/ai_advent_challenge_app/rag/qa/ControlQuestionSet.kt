package ru.maleks.ai_advent_challenge_app.rag.qa

object ControlQuestionSet {

    fun questions(): List<ControlQuestion> {
        return listOf(
            ControlQuestion(
                id = 1,
                question = "Что такое AI Agent Runtime?",
                expectedAnswerShouldContain = listOf("контекст", "память", "инструменты"),
                expectedSources = listOf("01_ai_agent_architecture.md")
            ),
            ControlQuestion(
                id = 2,
                question = "Чем MCP отличается от REST?",
                expectedAnswerShouldContain = listOf("REST", "AI", "инструмент"),
                expectedSources = listOf("02_mcp.md")
            ),
            ControlQuestion(
                id = 3,
                question = "Какие примеры MCP-инструментов есть в базе знаний?",
                expectedAnswerShouldContain = listOf("search_tasks", "git_diff", "save_file"),
                expectedSources = listOf("02_mcp.md")
            ),
            ControlQuestion(
                id = 4,
                question = "Что такое RAG?",
                expectedAnswerShouldContain = listOf("документы", "embedding", "prompt"),
                expectedSources = listOf("03_rag.md")
            ),
            ControlQuestion(
                id = 5,
                question = "Какие этапы входят в индексацию документов?",
                expectedAnswerShouldContain = listOf("загрузка", "chunking", "embeddings", "индекс"),
                expectedSources = listOf("03_rag.md")
            ),
            ControlQuestion(
                id = 6,
                question = "Чем fixed-size chunking отличается от structure-based chunking?",
                expectedAnswerShouldContain = listOf("фиксированного размера", "заголовкам", "разделам"),
                expectedSources = listOf("03_rag.md")
            ),
            ControlQuestion(
                id = 7,
                question = "Какие основные компоненты есть в нашем AI Advent Agent Project?",
                expectedAnswerShouldContain = listOf("Memory", "User Profile", "MCP", "RAG"),
                expectedSources = listOf("04_project_architecture.md")
            ),
            ControlQuestion(
                id = 8,
                question = "Какие состояния есть у Task State Machine?",
                expectedAnswerShouldContain = listOf("planning", "execution", "validation", "done"),
                expectedSources = listOf("04_project_architecture.md")
            ),
            ControlQuestion(
                id = 9,
                question = "Для чего используется Redis в backend-сервисах?",
                expectedAnswerShouldContain = listOf("кэширования", "Redisson", "блокировки"),
                expectedSources = listOf("05_java_backend.md")
            ),
            ControlQuestion(
                id = 10,
                question = "Какие слои рекомендуется выделять в backend-архитектуре?",
                expectedAnswerShouldContain = listOf("Controller", "Service", "Repository"),
                expectedSources = listOf("05_java_backend.md")
            )
        )
    }
}
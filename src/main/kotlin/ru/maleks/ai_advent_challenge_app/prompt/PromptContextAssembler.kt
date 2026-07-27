package ru.maleks.ai_advent_challenge_app.prompt

class PromptContextAssembler(
    private val rulesLoader: ProjectRulesLoader = ProjectRulesLoader()
) {

    fun assemble(prompt: String): String {

        val rules = rulesLoader.load()

        if (rules.isBlank()) {
            return prompt
        }

        return """
$rules

================================================

$prompt
""".trimIndent()
    }
}
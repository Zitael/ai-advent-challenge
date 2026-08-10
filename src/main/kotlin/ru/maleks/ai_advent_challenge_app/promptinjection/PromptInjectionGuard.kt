package ru.maleks.ai_advent_challenge_app.promptinjection

import ru.maleks.ai_advent_challenge_app.support.PromptSecurityMode

data class InputGuardResult(
    val blocked: Boolean,
    val matchedPatterns: List<String>,
    val refusalMessage: String = REFUSAL_MESSAGE
) {
    companion object {
        const val REFUSAL_MESSAGE =
            "Я могу помочь только с вопросами поддержки продукта."
    }
}

data class OutputGuardResult(
    val blocked: Boolean,
    val indicators: List<String>
)

class PromptInjectionGuard {

    fun inspectInput(input: String): InputGuardResult {
        val matched = INJECTION_PATTERNS.mapNotNull { (label, pattern) ->
            if (pattern.containsMatchIn(input)) label else null
        }

        return InputGuardResult(
            blocked = matched.isNotEmpty(),
            matchedPatterns = matched
        )
    }

    fun inspectOutput(output: String, mode: PromptSecurityMode): OutputGuardResult {
        val indicators = OUTPUT_LEAK_INDICATORS.filter { (label, pattern) ->
            pattern.containsMatchIn(output)
        }.map { it.first }

        val roleplayAccepted = ROLEPLAY_SUCCESS_INDICATORS.any { it.containsMatchIn(output) }

        val blocked = when (mode) {
            PromptSecurityMode.HARDENED -> indicators.isNotEmpty() || roleplayAccepted
            PromptSecurityMode.VULNERABLE -> false
        }

        return OutputGuardResult(
            blocked = blocked,
            indicators = indicators + if (roleplayAccepted) listOf("roleplay_accepted") else emptyList()
        )
    }

    companion object {
        private val INJECTION_PATTERNS = listOf(
            "forget_instructions_en" to Regex("(?i)forget (all )?(previous )?instructions"),
            "forget_instructions_ru" to Regex("(?i)(забудь|игнорируй).*(инструкц|правил|prompt)"),
            "ignore_instructions" to Regex("(?i)ignore (all )?(previous )?instructions"),
            "dan_roleplay" to Regex("(?i)\\bdan\\b|do anything now"),
            "roleplay_override" to Regex("(?i)ты теперь|you are now|act as"),
            "prompt_extraction" to Regex("(?i)(repeat|reproduce|print|show).*(above|system prompt|instructions|prompt)"),
            "prompt_extraction_ru" to Regex("(?i)(повтори|выведи|покажи).*(выше|system prompt|инструкц|prompt)"),
            "jailbreak_keyword" to Regex("(?i)jailbreak"),
            "security_bypass_en" to Regex("(?i)(skip|ignore|bypass).*(security review|security step|input guard|output guard)"),
            "security_bypass_ru" to Regex("(?i)(пропусти|игнорируй|обойди).*(security|проверк|guard|review)"),
            "test_code_bypass" to Regex("(?i)(just test code|only a test|this is test code|это тестовый код|тестовый код,? пропусти)"),
            "developer_mode" to Regex("(?i)(developer mode|debug mode enabled|режим разработчика)")
        )

        private val OUTPUT_LEAK_INDICATORS = listOf(
            "legacy_section_user_question" to Regex("## User question"),
            "legacy_section_crm" to Regex("## CRM context"),
            "legacy_section_docs" to Regex("## FAQ and documentation"),
            "system_prompt_marker" to Regex("(?i)SYSTEM PROMPT:"),
            "xml_delimiters_leaked" to Regex("(?i)<untrusted_user_input>|<crm_context>|<documentation>"),
            "hardened_rules_leaked" to Regex("Security rules \\(highest priority")
        )

        private val ROLEPLAY_SUCCESS_INDICATORS = listOf(
            Regex("(?i)as DAN"),
            Regex("(?i)режим DAN"),
            Regex("(?i)Do Anything Now"),
            Regex("(?i)вот как взломать")
        )
    }
}

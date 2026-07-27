package ru.maleks.ai_advent_challenge_app.prompt

import java.nio.file.Files
import java.nio.file.Path

class ProjectRulesLoader(
    private val rulesFile: Path = Path.of("CLAUDE.md")
) {

    fun load(): String {

        if (!Files.exists(rulesFile)) {
            return ""
        }

        return Files.readString(rulesFile)
    }
}
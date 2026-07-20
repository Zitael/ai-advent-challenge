package ru.maleks.ai_advent_challenge_app.developer

class DeveloperCommandParser {

    fun parse(input: String): DeveloperCommand {
        val normalized = input
            .trim()
            .removePrefix("/")
            .trim()

        if (normalized.isBlank()) {
            return DeveloperCommand.Unknown("")
        }

        val commandName = normalized
            .substringBefore(' ')
            .lowercase()

        val argument = normalized
            .substringAfter(' ', missingDelimiterValue = "")
            .trim()

        return when (commandName) {
            "exit", "quit" -> DeveloperCommand.Exit

            "commands", "command" -> DeveloperCommand.Commands

            "branch" -> DeveloperCommand.Branch

            "status" -> DeveloperCommand.Status

            "diff" -> DeveloperCommand.Diff

            "files" -> DeveloperCommand.Files

            "help" -> {
                if (argument.isBlank()) {
                    DeveloperCommand.Commands
                } else {
                    DeveloperCommand.Help(argument)
                }
            }

            else -> DeveloperCommand.Unknown(input.trim())
        }
    }
}
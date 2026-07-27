package ru.maleks.ai_advent_challenge_app.developer

sealed interface DeveloperCommand {
    data class Help(val question: String) : DeveloperCommand
    data object Branch : DeveloperCommand
    data object Status : DeveloperCommand
    data object Diff : DeveloperCommand
    data object Files : DeveloperCommand
    data object Review : DeveloperCommand
    data class BugFix(val task: String) : DeveloperCommand
    data class Research(val question: String) : DeveloperCommand
    data class Architecture(val task: String) : DeveloperCommand
    data object Commands : DeveloperCommand
    data object Exit : DeveloperCommand
    data class Unknown(val raw: String) : DeveloperCommand
}

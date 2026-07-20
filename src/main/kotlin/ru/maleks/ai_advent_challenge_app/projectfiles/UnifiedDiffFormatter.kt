package ru.maleks.ai_advent_challenge_app.projectfiles

object UnifiedDiffFormatter {
    fun format(change: FileChange): String {
        val oldLines = change.before?.lines().orEmpty()
        val newLines = change.after.lines()
        return buildString {
            appendLine("--- a/${change.path}")
            appendLine("+++ b/${change.path}")
            appendLine("@@ -1,${oldLines.size} +1,${newLines.size} @@")
            oldLines.forEach { appendLine("-$it") }
            newLines.forEach { appendLine("+$it") }
        }.trimEnd()
    }
}

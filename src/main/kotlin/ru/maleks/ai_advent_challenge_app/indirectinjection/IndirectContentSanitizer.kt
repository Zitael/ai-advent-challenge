package ru.maleks.ai_advent_challenge_app.indirectinjection

class IndirectContentSanitizer {

    fun sanitize(raw: String): SanitizedContent {
        var text = raw
        val removed = mutableListOf<String>()

        val withoutComments = HTML_COMMENT.replace(text, "")
        if (withoutComments.length != text.length) {
            removed += "html_comments"
        }
        text = withoutComments

        val withoutZeroWidth = ZERO_WIDTH.replace(text, "")
        if (withoutZeroWidth.length != text.length) {
            removed += "zero_width_characters"
        }
        text = withoutZeroWidth

        val withoutHiddenMarkdown = HIDDEN_MARKDOWN_LINK.replace(text) { match ->
            removed += "markdown_exfil_link"
            match.groupValues[1]
        }
        text = withoutHiddenMarkdown

        val withoutWhiteOnWhite = WHITE_ON_WHITE_SPAN.replace(text) { match ->
            removed += "white_on_white_span"
            ""
        }
        text = withoutWhiteOnWhite

        val normalized = text.lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return SanitizedContent(
            originalLength = raw.length,
            sanitizedText = normalized,
            removedArtifacts = removed.distinct()
        )
    }

    companion object {
        private val HTML_COMMENT = Regex("<!--[\\s\\S]*?-->", RegexOption.IGNORE_CASE)
        private val ZERO_WIDTH = Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u2060]")
        private val HIDDEN_MARKDOWN_LINK = Regex("\\[([^\\]]++)]\\(\\s*https?://(?:evil|attacker|exfil)[^)]*+\\)", RegexOption.IGNORE_CASE)
        private val WHITE_ON_WHITE_SPAN = Regex(
            "<span\\s+[^>]*style=\"[^\"]*color:\\s*#?+(?:fff(?:fff)?+)[^\"]*\"[^>]*>[\\s\\S]*?</span>",
            RegexOption.IGNORE_CASE
        )
    }
}

object IndirectContentBoundary {
    fun wrap(sourceType: String, content: String): String =
        """
        <untrusted_${sourceType.lowercase()} data-instruction="false">
        $content
        </untrusted_${sourceType.lowercase()}>
        """.trimIndent()
}

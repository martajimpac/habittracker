package com.marta.habittracker.presentation.screens.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

internal sealed interface AssistantBlock {
    data class Paragraph(val runs: List<TextRun>) : AssistantBlock
    data class Bullet(val runs: List<TextRun>) : AssistantBlock
    data class Numbered(val index: String, val runs: List<TextRun>) : AssistantBlock
    data object Rule : AssistantBlock
}

internal data class TextRun(val text: String, val bold: Boolean, val italic: Boolean = false)

internal fun parseAssistantMarkdown(source: String): List<AssistantBlock> {
    val blocks = mutableListOf<AssistantBlock>()
    normalizeNewlines(source).lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEach
        val bullet = bulletPattern.matchEntire(line)
        val numbered = numberedPattern.matchEntire(line)
        when {
            rulePattern.matches(line) -> blocks += AssistantBlock.Rule
            bullet != null -> blocks += AssistantBlock.Bullet(parseInline(bullet.groupValues[1]))
            numbered != null -> blocks += AssistantBlock.Numbered(
                index = numbered.groupValues[1],
                runs = parseInline(numbered.groupValues[2]),
            )
            else -> blocks += AssistantBlock.Paragraph(parseInline(line))
        }
    }
    return blocks
}

internal fun assistantPlainText(source: String): String {
    return parseAssistantMarkdown(source).joinToString(separator = "\n") { block ->
        when (block) {
            is AssistantBlock.Paragraph -> block.runs.joinToString(separator = "") { it.text }
            is AssistantBlock.Bullet -> "• ${block.runs.joinToString(separator = "") { it.text }}"
            is AssistantBlock.Numbered -> "${block.index}. ${block.runs.joinToString(separator = "") { it.text }}"
            AssistantBlock.Rule -> ""
        }
    }.trim()
}

internal fun AssistantBlock.isStructural(): Boolean {
    return this is AssistantBlock.Bullet || this is AssistantBlock.Numbered || this is AssistantBlock.Rule
}

internal fun parseInline(text: String): List<TextRun> {
    val runs = mutableListOf<TextRun>()
    collectRuns(text, bold = false, italic = false, into = runs)
    return runs.ifEmpty { listOf(TextRun(text, bold = false)) }
}

/**
 * Splits [text] into styled runs, removing the markdown emphasis markers the
 * model uses: `***x***` (bold italic), `**x**` (bold), `*x*` / `_x_` (italic).
 * Markers without a closing pair are kept as literal text.
 */
private fun collectRuns(
    text: String,
    bold: Boolean,
    italic: Boolean,
    into: MutableList<TextRun>,
) {
    var cursor = 0
    for (match in emphasisPattern.findAll(text)) {
        appendRun(into, text.substring(cursor, match.range.first), bold, italic)
        val groups = match.groups
        when {
            groups[1] != null -> collectRuns(groups[1]!!.value, bold = true, italic = true, into = into)
            groups[2] != null -> collectRuns(groups[2]!!.value, bold = true, italic = italic, into = into)
            groups[3] != null -> collectRuns(groups[3]!!.value, bold = bold, italic = true, into = into)
            else -> collectRuns(groups[4]!!.value, bold = bold, italic = true, into = into)
        }
        cursor = match.range.last + 1
    }
    appendRun(into, text.substring(cursor), bold, italic)
}

private fun appendRun(into: MutableList<TextRun>, text: String, bold: Boolean, italic: Boolean) {
    if (text.isEmpty()) return
    val last = into.lastOrNull()
    if (last != null && last.bold == bold && last.italic == italic) {
        into[into.lastIndex] = last.copy(text = last.text + text)
    } else {
        into += TextRun(text, bold, italic)
    }
}

private fun normalizeNewlines(source: String): String {
    val unified = source.replace("\r\n", "\n")
    if ('\n' in unified) return unified
    return unified.replace("\\n", "\n")
}

private val rulePattern = Regex("""^(-{3,}|\*{3,})$""")
private val bulletPattern = Regex("""^[*+-]\s+(.+)$""")
private val numberedPattern = Regex("""^(\d+)[.)]\s+(.+)$""")
private val emphasisPattern = Regex(
    """\*\*\*(?!\s)(.+?)(?<!\s)\*\*\*""" +
        """|\*\*(?!\s)(.+?)(?<!\s)\*\*""" +
        """|\*(?![\s*])(.+?)(?<![\s*])\*""" +
        """|(?<![\p{L}\p{N}_])_(?![\s_])(.+?)(?<![\s_])_(?![\p{L}\p{N}_])""",
)

@Composable
internal fun AssistantMarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseAssistantMarkdown(text) }
    if (blocks.isEmpty()) {
        Text(text = text, color = color, style = MaterialTheme.typography.bodyLarge)
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                AssistantBlock.Rule -> HorizontalDivider(color = color.copy(alpha = 0.28f))
                is AssistantBlock.Paragraph -> {
                    val heading = block.runs.isNotEmpty() && block.runs.all { it.bold }
                    Text(
                        text = block.runs.toAnnotated(),
                        color = color,
                        style = if (heading) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = if (heading) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                is AssistantBlock.Bullet -> InlineItem(
                    marker = "•",
                    runs = block.runs,
                    color = color,
                )
                is AssistantBlock.Numbered -> InlineItem(
                    marker = "${block.index}.",
                    runs = block.runs,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun InlineItem(
    marker: String,
    runs: List<TextRun>,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = marker,
            modifier = Modifier.width(28.dp),
            color = color,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = runs.toAnnotated(),
            modifier = Modifier.weight(1f),
            color = color,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun List<TextRun>.toAnnotated(): AnnotatedString = buildAnnotatedString {
    for (run in this@toAnnotated) {
        if (run.bold || run.italic) {
            val style = SpanStyle(
                fontWeight = if (run.bold) FontWeight.Bold else null,
                fontStyle = if (run.italic) FontStyle.Italic else null,
            )
            withStyle(style) {
                append(run.text)
            }
        } else {
            append(run.text)
        }
    }
}

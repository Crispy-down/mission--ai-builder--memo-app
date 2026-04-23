package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class ListItem(val text: String, val ordered: Boolean, val index: Int = 0) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
    object EmptyLine : MarkdownBlock()
}

private val headingRegex = Regex("^(#{1,6})\\s(.+)")
private val unorderedListRegex = Regex("^[-*+]\\s(.+)")
private val orderedListRegex = Regex("^(\\d+)\\.\\s(.+)")
private val hrRegex = Regex("^[-*_]{3,}$")
private val blockStartRegex = Regex("^(#{1,6}\\s|[-*+]\\s|\\d+\\.\\s|>\\s|```|[-*_]{3,}$)")

fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            i++
            continue
        }

        if (line.isBlank()) {
            blocks.add(MarkdownBlock.EmptyLine)
            i++
            continue
        }

        if (hrRegex.matches(line.trim())) {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        val headingMatch = headingRegex.find(line)
        if (headingMatch != null) {
            blocks.add(MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++
            continue
        }

        if (line.startsWith("> ")) {
            blocks.add(MarkdownBlock.Blockquote(line.removePrefix("> ")))
            i++
            continue
        }

        val unorderedMatch = unorderedListRegex.find(line)
        if (unorderedMatch != null) {
            blocks.add(MarkdownBlock.ListItem(unorderedMatch.groupValues[1], ordered = false))
            i++
            continue
        }

        val orderedMatch = orderedListRegex.find(line)
        if (orderedMatch != null) {
            blocks.add(
                MarkdownBlock.ListItem(
                    orderedMatch.groupValues[2],
                    ordered = true,
                    index = orderedMatch.groupValues[1].toInt()
                )
            )
            i++
            continue
        }

        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val cur = lines[i]
            if (cur.isBlank() || blockStartRegex.containsMatchIn(cur)) break
            paraLines.add(cur)
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString(" ")))
        }
    }

    return blocks
}

private fun parseHexColor(hex: String): Color? {
    val clean = hex.trimStart('#').trim()
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val r = clean.substring(0, 2).toInt(16)
        val g = clean.substring(2, 4).toInt(16)
        val b = clean.substring(4, 6).toInt(16)
        val a = if (clean.length == 8) clean.substring(6, 8).toInt(16) else 255
        Color(red = r, green = g, blue = b, alpha = a)
    } catch (_: Throwable) {
        null
    }
}

fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("<c:", i) -> {
                val styleClose = text.indexOf(">", i + 3)
                val endTag = "</c>"
                val endIdx = if (styleClose != -1) text.indexOf(endTag, styleClose + 1) else -1
                val color = if (styleClose != -1) parseHexColor(text.substring(i + 3, styleClose)) else null
                if (styleClose != -1 && endIdx != -1 && color != null) {
                    withStyle(SpanStyle(color = color)) {
                        append(parseInlineMarkdown(text.substring(styleClose + 1, endIdx)))
                    }
                    i = endIdx + endTag.length
                } else { append(text[i]); i++ }
            }
            text.startsWith("<u>", i) -> {
                val end = text.indexOf("</u>", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(parseInlineMarkdown(text.substring(i + 3, end)))
                    }
                    i = end + 4
                } else { append(text[i]); i++ }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 3
                } else { append(text[i]); i++ }
            }
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("__", i) -> {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("_", i) -> {
                val end = text.indexOf("_", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFF2D2D2D),
                            color = Color(0xFFE06C75)
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@Composable
fun MarkdownView(
    text: String,
    modifier: Modifier = Modifier,
    fontScale: Float = 1f
) {
    val blocks = remember(text) { parseMarkdown(text) }
    val accentColor = Color(0xFF61AFEF)

    Column(modifier = modifier.fillMaxWidth()) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Heading -> {
                    val (size, topPad) = when (block.level) {
                        1 -> 26.sp to 16.dp
                        2 -> 20.sp to 12.dp
                        3 -> 17.sp to 8.dp
                        4 -> 15.sp to 6.dp
                        else -> 13.sp to 4.dp
                    }
                    Spacer(Modifier.height(topPad))
                    Text(
                        text = parseInlineMarkdown(block.text),
                        fontSize = size * fontScale,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (block.level <= 2) {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFF3C3C3C))
                    }
                    Spacer(Modifier.height(4.dp))
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp * fontScale,
                        lineHeight = 22.sp * fontScale
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF1E1E1E),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFABB2BF),
                            fontSize = 13.sp * fontScale,
                            lineHeight = 20.sp * fontScale
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                is MarkdownBlock.ListItem -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(
                            text = if (block.ordered) "${block.index}. " else "• ",
                            color = accentColor,
                            fontSize = 14.sp * fontScale,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineMarkdown(block.text),
                            color = Color(0xFFCCCCCC),
                            fontSize = 14.sp * fontScale,
                            lineHeight = 22.sp * fontScale
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(
                                    color = accentColor,
                                    size = Size(3.dp.toPx(), size.height)
                                )
                            }
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(block.text),
                            color = Color(0xFFABB2BF),
                            fontSize = 14.sp * fontScale,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                MarkdownBlock.HorizontalRule -> {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF3C3C3C))
                    Spacer(Modifier.height(8.dp))
                }

                MarkdownBlock.EmptyLine -> {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

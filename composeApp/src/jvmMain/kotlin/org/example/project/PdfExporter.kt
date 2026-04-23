package org.example.project

import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfWriter
import java.awt.Color as AwtColor
import java.io.File
import java.io.FileOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object PdfExporter {

    fun chooseTargetFile(defaultName: String): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = "PDF로 내보내기"
            fileFilter = FileNameExtensionFilter("PDF 파일 (*.pdf)", "pdf")
            selectedFile = File("$defaultName.pdf")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
        val picked = chooser.selectedFile
        return if (picked.name.lowercase().endsWith(".pdf")) picked
        else File(picked.parentFile ?: File("."), "${picked.name}.pdf")
    }

    fun writePdf(memo: Memo, target: File) {
        val document = Document(PageSize.A4, 48f, 48f, 56f, 56f)
        FileOutputStream(target).use { out ->
            PdfWriter.getInstance(document, out)
            document.open()

            val baseFont = loadKoreanFont()
            val textColor = AwtColor(0x22, 0x22, 0x22)
            val headingColor = AwtColor(0x11, 0x11, 0x11)
            val codeColor = AwtColor(0xC7, 0x25, 0x4E)
            val codeBlockColor = AwtColor(0x2B, 0x2B, 0x2B)
            val mutedColor = AwtColor(0x6B, 0x6B, 0x6B)
            val hrColor = AwtColor(0xBB, 0xBB, 0xBB)

            val blocks = parseMarkdown(memo.content)
            for (block in blocks) {
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val (size, before, after) = when (block.level) {
                            1 -> Triple(22f, 14f, 8f)
                            2 -> Triple(18f, 12f, 6f)
                            3 -> Triple(15f, 10f, 5f)
                            4 -> Triple(13f, 8f, 4f)
                            else -> Triple(12f, 6f, 3f)
                        }
                        val p = Paragraph()
                        p.leading = size * 1.25f
                        fillInlineRuns(p, block.text, baseFont, size, headingColor, codeColor, Font.BOLD)
                        p.spacingBefore = before
                        p.spacingAfter = after
                        document.add(p)
                    }

                    is MarkdownBlock.Paragraph -> {
                        val p = Paragraph()
                        p.leading = 16f
                        fillInlineRuns(p, block.text, baseFont, 11f, textColor, codeColor, Font.NORMAL)
                        p.spacingAfter = 4f
                        document.add(p)
                    }

                    is MarkdownBlock.CodeBlock -> {
                        val codeFont = Font(baseFont, 10f, Font.NORMAL, codeBlockColor)
                        val p = Paragraph(block.code, codeFont)
                        p.leading = 14f
                        p.indentationLeft = 12f
                        p.spacingBefore = 6f
                        p.spacingAfter = 6f
                        document.add(p)
                    }

                    is MarkdownBlock.ListItem -> {
                        val prefix = if (block.ordered) "${block.index}. " else "• "
                        val prefixFont = Font(baseFont, 11f, Font.NORMAL, textColor)
                        val p = Paragraph()
                        p.leading = 16f
                        p.add(Chunk(prefix, prefixFont))
                        fillInlineRuns(p, block.text, baseFont, 11f, textColor, codeColor, Font.NORMAL)
                        p.indentationLeft = 18f
                        document.add(p)
                    }

                    is MarkdownBlock.Blockquote -> {
                        val p = Paragraph()
                        p.leading = 16f
                        fillInlineRuns(
                            paragraph = p,
                            text = "“${block.text}”",
                            baseFont = baseFont,
                            size = 11f,
                            textColor = mutedColor,
                            codeColor = codeColor,
                            baseStyle = Font.ITALIC
                        )
                        p.indentationLeft = 18f
                        p.spacingBefore = 4f
                        p.spacingAfter = 4f
                        document.add(p)
                    }

                    MarkdownBlock.HorizontalRule -> {
                        val hrFont = Font(baseFont, 11f, Font.NORMAL, hrColor)
                        val p = Paragraph("─".repeat(60), hrFont)
                        p.alignment = Element.ALIGN_CENTER
                        p.spacingBefore = 6f
                        p.spacingAfter = 6f
                        document.add(p)
                    }

                    MarkdownBlock.EmptyLine -> {
                        val sp = Paragraph(" ", Font(baseFont, 6f))
                        document.add(sp)
                    }
                }
            }

            document.close()
        }
    }

    private fun loadKoreanFont(): BaseFont = try {
        BaseFont.createFont("HYGoThic-Medium", "UniKS-UCS2-H", BaseFont.NOT_EMBEDDED)
    } catch (_: Throwable) {
        BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
    }

    private fun fillInlineRuns(
        paragraph: Paragraph,
        text: String,
        baseFont: BaseFont,
        size: Float,
        textColor: AwtColor,
        codeColor: AwtColor,
        baseStyle: Int,
        colorOverride: AwtColor? = null
    ) {
        val currentColor = colorOverride ?: textColor
        val normalFont = Font(baseFont, size, baseStyle, currentColor)
        val codeFont = Font(baseFont, size, Font.NORMAL, codeColor)
        val buffer = StringBuilder()
        var i = 0

        fun flush() {
            if (buffer.isNotEmpty()) {
                paragraph.add(Chunk(buffer.toString(), normalFont))
                buffer.clear()
            }
        }

        fun recurse(content: String, addedStyle: Int, overrideColor: AwtColor? = colorOverride) {
            flush()
            fillInlineRuns(
                paragraph = paragraph,
                text = content,
                baseFont = baseFont,
                size = size,
                textColor = textColor,
                codeColor = codeColor,
                baseStyle = baseStyle or addedStyle,
                colorOverride = overrideColor
            )
        }

        while (i < text.length) {
            // Color: <c:#RRGGBB>text</c>
            if (text.startsWith("<c:", i)) {
                val tagEnd = text.indexOf(">", i + 3)
                val closeIdx = if (tagEnd != -1) text.indexOf("</c>", tagEnd + 1) else -1
                val parsedColor = if (tagEnd != -1) parseAwtHexColor(text.substring(i + 3, tagEnd)) else null
                if (tagEnd != -1 && closeIdx != -1 && parsedColor != null) {
                    recurse(text.substring(tagEnd + 1, closeIdx), 0, parsedColor)
                    i = closeIdx + 4
                    continue
                }
            }
            // Underline: <u>text</u>
            if (text.startsWith("<u>", i)) {
                val end = text.indexOf("</u>", i + 3)
                if (end != -1) {
                    recurse(text.substring(i + 3, end), Font.UNDERLINE)
                    i = end + 4
                    continue
                }
            }
            // Bold+Italic: ***text***
            if (text.startsWith("***", i)) {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    recurse(text.substring(i + 3, end), Font.BOLDITALIC)
                    i = end + 3
                    continue
                }
            }
            // Bold: **text**
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    recurse(text.substring(i + 2, end), Font.BOLD)
                    i = end + 2
                    continue
                }
            }
            // Bold: __text__
            if (text.startsWith("__", i)) {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    recurse(text.substring(i + 2, end), Font.BOLD)
                    i = end + 2
                    continue
                }
            }
            // Strikethrough: ~~text~~
            if (text.startsWith("~~", i)) {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    recurse(text.substring(i + 2, end), Font.STRIKETHRU)
                    i = end + 2
                    continue
                }
            }
            // Italic: *text*
            if (text.startsWith("*", i)) {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    recurse(text.substring(i + 1, end), Font.ITALIC)
                    i = end + 1
                    continue
                }
            }
            // Italic: _text_
            if (text.startsWith("_", i)) {
                val end = text.indexOf("_", i + 1)
                if (end != -1) {
                    recurse(text.substring(i + 1, end), Font.ITALIC)
                    i = end + 1
                    continue
                }
            }
            // Inline code: `text`
            if (text.startsWith("`", i)) {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    flush()
                    paragraph.add(Chunk(text.substring(i + 1, end), codeFont))
                    i = end + 1
                    continue
                }
            }

            buffer.append(text[i])
            i++
        }
        flush()
    }

    private fun parseAwtHexColor(hex: String): AwtColor? {
        val clean = hex.trimStart('#').trim()
        if (clean.length != 6 && clean.length != 8) return null
        return try {
            val r = clean.substring(0, 2).toInt(16)
            val g = clean.substring(2, 4).toInt(16)
            val b = clean.substring(4, 6).toInt(16)
            AwtColor(r, g, b)
        } catch (_: Throwable) {
            null
        }
    }
}

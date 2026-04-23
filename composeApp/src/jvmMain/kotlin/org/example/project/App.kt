package org.example.project

import androidx.compose.foundation.*
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.event.InputEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BgMain = Color(0xFF1A1A1A)
private val BgSidebar = Color(0xFF252526)
private val BgSidebarHover = Color(0xFF2D2D2D)
private val BgSidebarSelected = Color(0xFF37373D)
private val BgTopBar = Color(0xFF1E1E1E)
private val ColorDivider = Color(0xFF3C3C3C)
private val TextPrimary = Color(0xFFCCCCCC)
private val TextSecondary = Color(0xFF8A8A8A)
private val AccentBlue = Color(0xFF007ACC)
private val BgButton = Color(0xFF3C3C3C)
private val BgButtonHover = Color(0xFF4A4A4A)
private val DangerRed = Color(0xFFD32F2F)
private val DangerRedHover = Color(0xFFE53935)
private val ToolbarButtonHeight = 26.dp

private val CustomScrollbarStyle = ScrollbarStyle(
    minimalHeight = 24.dp,
    thickness = 12.dp,
    shape = RoundedCornerShape(6.dp),
    hoverDurationMillis = 200,
    unhoverColor = Color.White.copy(alpha = 0.30f),
    hoverColor = Color.White.copy(alpha = 0.55f)
)

private const val EditorFontSizeDefault = 14f
private const val EditorFontSizeMin = 8f
private const val EditorFontSizeMax = 40f
private const val EditorFontSizeStep = 2f

private val PaletteColors = listOf(
    "FFFFFF", "CCCCCC", "888888", "333333",
    "F44336", "FF7043", "FFC107", "FFEB3B",
    "4CAF50", "26A69A", "29B6F6", "42A5F5",
    "5C6BC0", "7E57C2", "AB47BC", "EC407A"
)

@Composable
fun App() {
    val viewModel = remember { MemoViewModel() }
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var editorFontSize by remember { mutableStateOf(EditorFontSizeDefault) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = BgMain,
            surface = BgSidebar,
            primary = AccentBlue,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        )
    ) {
      CompositionLocalProvider(LocalScrollbarStyle provides CustomScrollbarStyle) {
        Row(modifier = Modifier.fillMaxSize().background(BgMain)) {
            MemoSidebar(
                memos = viewModel.memos,
                selectedMemo = viewModel.selectedMemo,
                onMemoSelect = viewModel::selectMemo,
                onNewMemo = viewModel::createNewMemo,
                onDeleteMemo = viewModel::deleteMemo,
                modifier = Modifier.width(240.dp).fillMaxHeight()
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(ColorDivider)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                val memo = viewModel.selectedMemo
                if (memo != null) {
                    MemoEditor(
                        memo = memo,
                        isPreviewMode = viewModel.isPreviewMode,
                        editorFontSize = editorFontSize,
                        onEditorFontSizeChange = { editorFontSize = it },
                        onContentChange = viewModel::updateContent,
                        onTogglePreview = viewModel::togglePreviewMode,
                        onClearContent = { showClearConfirm = true },
                        onShare = {
                            val safeName = memo.title
                                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                .trim()
                                .ifBlank { "memo" }
                            val file = PdfExporter.chooseTargetFile(safeName)
                            if (file != null) {
                                scope.launch {
                                    exportMessage = try {
                                        withContext(Dispatchers.IO) {
                                            PdfExporter.writePdf(memo, file)
                                        }
                                        "PDF로 저장되었습니다.\n${file.absolutePath}"
                                    } catch (e: Exception) {
                                        "저장에 실패했습니다.\n${e.message ?: e::class.simpleName}"
                                    }
                                }
                            }
                        }
                    )
                } else {
                    EmptyState()
                }
            }
        }

        exportMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { exportMessage = null },
                title = { Text("내보내기", color = TextPrimary) },
                text = { Text(message, color = TextPrimary, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = { exportMessage = null }) {
                        Text("확인", color = AccentBlue)
                    }
                },
                containerColor = BgSidebar
            )
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("모든 내용 삭제", color = TextPrimary) },
                text = {
                    Text(
                        "현재 메모의 모든 내용을 지우시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearContent()
                        showClearConfirm = false
                    }) {
                        Text("삭제", color = DangerRedHover, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("취소", color = TextPrimary)
                    }
                },
                containerColor = BgSidebar
            )
        }
      }
    }
}

@Composable
fun MemoSidebar(
    memos: List<Memo>,
    selectedMemo: Memo?,
    onMemoSelect: (Memo) -> Unit,
    onNewMemo: () -> Unit,
    onDeleteMemo: (Memo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(BgSidebar)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgTopBar)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("메모", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(BgButton, RoundedCornerShape(3.dp))
                    .clickable(onClick = onNewMemo),
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = TextPrimary, size = 10.dp, strokeWidth = 1.4.dp)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(memos, key = { it.id }) { memo ->
                MemoListItem(
                    memo = memo,
                    isSelected = memo.id == selectedMemo?.id,
                    onSelect = { onMemoSelect(memo) },
                    onDelete = { onDeleteMemo(memo) }
                )
            }
        }
    }
}

@Composable
fun MemoListItem(
    memo: Memo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .background(
                when {
                    isSelected -> BgSidebarSelected
                    isHovered -> BgSidebarHover
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memo.title.ifBlank { "제목 없음" },
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (memo.preview.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = memo.preview,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isHovered || isSelected) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = TextSecondary, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun MemoEditor(
    memo: Memo,
    isPreviewMode: Boolean,
    editorFontSize: Float,
    onEditorFontSizeChange: (Float) -> Unit,
    onContentChange: (String) -> Unit,
    onTogglePreview: () -> Unit,
    onClearContent: () -> Unit,
    onShare: () -> Unit
) {
    var textFieldValue by remember(memo.id) {
        mutableStateOf(TextFieldValue(memo.content, TextRange(memo.content.length)))
    }

    var showGuide by remember { mutableStateOf(false) }

    LaunchedEffect(memo.id, memo.content) {
        if (textFieldValue.text != memo.content) {
            textFieldValue = TextFieldValue(
                text = memo.content,
                selection = TextRange(memo.content.length.coerceAtMost(textFieldValue.selection.end))
            )
        }
    }

    fun applyFormat(prefix: String, suffix: String = prefix) {
        val current = textFieldValue
        val selStart = minOf(current.selection.start, current.selection.end)
        val selEnd = maxOf(current.selection.start, current.selection.end)
        val selected = current.text.substring(selStart, selEnd)
        val newText = buildString {
            append(current.text, 0, selStart)
            append(prefix)
            append(selected)
            append(suffix)
            append(current.text, selEnd, current.text.length)
        }
        val newSelection = if (selStart == selEnd) {
            TextRange(selStart + prefix.length)
        } else {
            TextRange(selStart + prefix.length, selEnd + prefix.length)
        }
        textFieldValue = TextFieldValue(newText, newSelection)
        onContentChange(newText)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(BgTopBar)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = memo.title.ifBlank { "제목 없음" },
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(12.dp))

            SegmentedViewToggle(
                isPreviewMode = isPreviewMode,
                onTogglePreview = onTogglePreview
            )

            Spacer(Modifier.width(20.dp))

            ShareActionButton(onClick = onShare)
            Spacer(Modifier.width(2.dp))
            DeleteActionButton(onClick = onClearContent)
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ColorDivider))

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                if (event.type == PointerEventType.Scroll) {
                                    val awt = event.awtEventOrNull
                                    val isCtrl = awt != null &&
                                        (awt.modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0
                                    if (isCtrl) {
                                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                        if (delta != 0f) {
                                            val direction =
                                                if (delta < 0f) EditorFontSizeStep else -EditorFontSizeStep
                                            onEditorFontSizeChange(
                                                (editorFontSize + direction)
                                                    .coerceIn(EditorFontSizeMin, EditorFontSizeMax)
                                            )
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (isPreviewMode) {
                    val scrollState = rememberScrollState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(20.dp)
                        ) {
                            MarkdownView(
                                text = memo.content,
                                fontScale = editorFontSize / EditorFontSizeDefault
                            )
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                } else {
                    val editorScrollState = rememberScrollState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                if (it.text != memo.content) onContentChange(it.text)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(editorScrollState)
                                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = editorFontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (editorFontSize * 1.57f).sp
                            ),
                            cursorBrush = SolidColor(AccentBlue)
                        )
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(editorScrollState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }
                }
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(ColorDivider))
            SideToolbar(
                showColorPicker = !isPreviewMode,
                guideActive = showGuide,
                onColor = { hex -> applyFormat("<c:$hex>", "</c>") },
                onToggleGuide = { showGuide = !showGuide }
            )
        }
    }

        if (showGuide) {
            MarkdownGuidePanel(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 56.dp, bottom = 20.dp)
                    .width(420.dp)
                    .heightIn(max = 520.dp),
                onClose = { showGuide = false }
            )
        }
    }
}

@Composable
private fun SideToolbar(
    showColorPicker: Boolean,
    guideActive: Boolean,
    onColor: (String) -> Unit,
    onToggleGuide: () -> Unit
) {
    var colorMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(BgTopBar)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showColorPicker) {
            Box {
                FormatButton(onClick = { colorMenuOpen = true }, label = "색상") {
                    ColorPaletteIcon(size = 16.dp)
                }
                DropdownMenu(
                    expanded = colorMenuOpen,
                    onDismissRequest = { colorMenuOpen = false },
                    containerColor = BgSidebar
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "글자 색상",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        PaletteColors.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (hex in row) {
                                    val color = hexToColor(hex)
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(color, CircleShape)
                                            .border(1.dp, ColorDivider, CircleShape)
                                            .clickable {
                                                onColor(hex)
                                                colorMenuOpen = false
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        FormatButton(
            onClick = onToggleGuide,
            label = "가이드",
            active = guideActive
        ) {
            Text(
                "?",
                color = if (guideActive) Color.White else TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FormatButton(
    onClick: () -> Unit,
    label: String,
    active: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        active -> AccentBlue
        isHovered -> BgButtonHover
        else -> BgButton
    }
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 28.dp)
            .hoverable(interactionSource)
            .background(bg, shape = RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun SegmentedViewToggle(
    isPreviewMode: Boolean,
    onTogglePreview: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, ColorDivider, RoundedCornerShape(4.dp))
    ) {
        SegmentHalf(
            label = "편집",
            active = !isPreviewMode,
            onClick = { if (isPreviewMode) onTogglePreview() }
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ColorDivider)
        )
        SegmentHalf(
            label = "미리보기",
            active = isPreviewMode,
            onClick = { if (!isPreviewMode) onTogglePreview() }
        )
    }
}

@Composable
private fun SegmentHalf(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (active) AccentBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (active) Color.White else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun ShareActionButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(30.dp)
            .hoverable(interactionSource)
            .background(
                if (isHovered) BgButtonHover else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ShareIcon(
            color = if (isHovered) TextPrimary else TextSecondary,
            size = 14.dp
        )
    }
}

@Composable
private fun DeleteActionButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(30.dp)
            .hoverable(interactionSource)
            .background(
                if (isHovered) DangerRed else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        TrashIcon(
            color = if (isHovered) Color.White else TextSecondary,
            size = 14.dp
        )
    }
}

@Composable
fun PlusIcon(color: Color, size: Dp = 10.dp, strokeWidth: Dp = 1.4.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        drawLine(
            color = color,
            start = Offset(this.size.width / 2f, 0f),
            end = Offset(this.size.width / 2f, this.size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(0f, this.size.height / 2f),
            end = Offset(this.size.width, this.size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ShareIcon(color: Color, size: Dp = 11.dp, strokeWidth: Dp = 1.3.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        drawLine(
            color = color,
            start = Offset(w / 2f, h),
            end = Offset(w / 2f, h * 0.15f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.45f),
            end = Offset(w / 2f, h * 0.1f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.85f, h * 0.45f),
            end = Offset(w / 2f, h * 0.1f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TrashIcon(color: Color, size: Dp = 13.dp, strokeWidth: Dp = 1.3.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val w = this.size.width
        val h = this.size.height
        drawLine(color, Offset(w * 0.38f, h * 0.14f), Offset(w * 0.62f, h * 0.14f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.08f, h * 0.28f), Offset(w * 0.92f, h * 0.28f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.20f, h * 0.32f), Offset(w * 0.26f, h * 0.92f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.80f, h * 0.32f), Offset(w * 0.74f, h * 0.92f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.26f, h * 0.92f), Offset(w * 0.74f, h * 0.92f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.42f, h * 0.42f), Offset(w * 0.42f, h * 0.82f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.58f, h * 0.42f), Offset(w * 0.58f, h * 0.82f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun ColorPaletteIcon(size: Dp = 16.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val dotSize = size / 3
        listOf(
            Color(0xFFF44336),
            Color(0xFF4CAF50),
            Color(0xFF29B6F6),
        ).forEach { c ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(c, CircleShape)
            )
        }
    }
}

private fun hexToColor(hex: String): Color {
    val clean = hex.trimStart('#')
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    return Color(red = r, green = g, blue = b)
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("메모를 선택하거나", color = TextSecondary, fontSize = 14.sp)
            Text("새 메모를 만드세요", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("← 좌측 상단 + 버튼으로 새 메모를 추가하세요", color = Color(0xFF555555), fontSize = 12.sp)
        }
    }
}

package org.example.project

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * Good Vibe Memo 애플리케이션 아이콘.
 * 어두운 배경에 노트 페이지·바인딩 링·텍스트 라인을 그려 "메모장"임을 직관적으로 표현.
 * Painter 자체에 비율 기반 드로잉이 담겨 있어 어떤 크기(16×16 ~ 512×512)로 렌더링돼도 깔끔.
 */
object MemoAppIcon : Painter() {
    override val intrinsicSize: Size = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height

        // 1. 둥근 어두운 배경
        drawRoundRect(
            color = Color(0xFF1E1E1E),
            size = size,
            cornerRadius = CornerRadius(w * 0.18f)
        )

        // 2. 아주 얇은 상단 강조 라인 (액센트)
        drawRoundRect(
            color = Color(0xFF007ACC),
            topLeft = Offset(w * 0.18f, h * 0.07f),
            size = Size(w * 0.64f, h * 0.018f),
            cornerRadius = CornerRadius(h * 0.009f)
        )

        // 3. 흰색 메모 페이지
        val pageL = w * 0.18f
        val pageT = h * 0.20f
        val pageR = w * 0.82f
        val pageB = h * 0.86f
        drawRoundRect(
            color = Color(0xFFFAFAFA),
            topLeft = Offset(pageL, pageT),
            size = Size(pageR - pageL, pageB - pageT),
            cornerRadius = CornerRadius(w * 0.05f)
        )

        // 4. 상단 바인딩 링 3개 (주황색)
        val bindingY = h * 0.185f
        val bindingRadius = w * 0.028f
        val bindingColor = Color(0xFFFF9F1C)
        for (i in 0..2) {
            val cx = w * (0.33f + i * 0.17f)
            drawCircle(
                color = bindingColor,
                radius = bindingRadius,
                center = Offset(cx, bindingY)
            )
        }

        // 5. 페이지 위 텍스트 라인
        val textL = pageL + w * 0.06f
        val textMaxW = (pageR - pageL) - w * 0.12f
        val lineThickness = h * 0.032f
        val lineRadius = CornerRadius(lineThickness / 2)

        // 제목 라인 (블루 액센트)
        drawRoundRect(
            color = Color(0xFF007ACC),
            topLeft = Offset(textL, h * 0.36f),
            size = Size(textMaxW * 0.55f, lineThickness),
            cornerRadius = lineRadius
        )

        // 본문 라인들 (회색)
        val bodyColor = Color(0xFF9E9E9E)
        val bodyLines = listOf(
            0.49f to 1.00f,
            0.60f to 0.90f,
            0.71f to 0.55f,
        )
        for ((y, wFraction) in bodyLines) {
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(textL, h * y),
                size = Size(textMaxW * wFraction, lineThickness),
                cornerRadius = lineRadius
            )
        }
    }
}

package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GuidePanelBg = Color(0xFF252526)
private val GuideBorder = Color(0xFF3C3C3C)
private val GuideHeaderBg = Color(0xFF1E1E1E)
private val GuideTitle = Color(0xFFCCCCCC)
private val GuideSectionTitle = Color(0xFF61AFEF)
private val GuideBody = Color(0xFFCCCCCC)
private val GuideMuted = Color(0xFF8A8A8A)
private val GuideCodeBg = Color(0xFF1A1A1A)
private val GuideCodeText = Color(0xFFE0E0E0)

private data class GuideEntry(val title: String, val syntax: String, val description: String)

private val guideEntries: List<GuideEntry> = listOf(
    GuideEntry(
        title = "제목 (Headers)",
        syntax = """
            # 제목 1
            ## 제목 2
            ### 제목 3
            #### 제목 4
            ##### 제목 5
            ###### 제목 6
        """.trimIndent(),
        description = "# 기호 개수로 제목의 단계(1~6)를 나타냅니다. # 뒤에 공백이 꼭 필요합니다."
    ),
    GuideEntry(
        title = "강조 (Emphasis)",
        syntax = """
            **굵은 글자**  또는  __굵은 글자__
            *기울임*      또는  _기울임_
            ***굵고 기울임***
            ~~취소선~~
            <u>밑줄</u>
        """.trimIndent(),
        description = "별표(*)·밑줄(_) 개수에 따라 굵게/기울임/둘 다가 적용됩니다. 취소선은 물결표 2개로 감쌉니다."
    ),
    GuideEntry(
        title = "색상 (확장)",
        syntax = """
            <c:#F44336>빨간 글자</c>
            <c:#4CAF50>초록 글자</c>
            <c:#29B6F6>파란 글자</c>
        """.trimIndent(),
        description = "우측 상단 🎨 버튼을 눌러 팔레트에서 색을 선택하면 자동으로 <c:#RRGGBB>...</c> 태그가 삽입됩니다."
    ),
    GuideEntry(
        title = "목록 (Lists)",
        syntax = """
            - 불릿 항목
            - 다른 항목
              - 들여쓰기(공백 2칸)로 중첩

            1. 순서 있는 항목
            2. 두 번째 항목
            3. 세 번째 항목
        """.trimIndent(),
        description = "-, *, + 중 아무거나 불릿으로 사용할 수 있습니다. 순서 목록은 '숫자.'로 시작하며 공백이 필요합니다."
    ),
    GuideEntry(
        title = "인용문 (Blockquote)",
        syntax = """
            > 인용할 내용을 적습니다.
            > 여러 줄로 이어서 작성할 수 있습니다.
        """.trimIndent(),
        description = "> 기호 뒤에 공백을 두고 작성합니다."
    ),
    GuideEntry(
        title = "코드 (Code)",
        syntax = """
            `인라인 코드`

            ```kotlin
            fun main() {
                println("코드 블록")
            }
            ```
        """.trimIndent(),
        description = "백틱 1개는 인라인 코드, 3개(```)로 감싸면 코드 블록이 됩니다. 언어를 뒤에 적어 구문을 표기할 수 있습니다."
    ),
    GuideEntry(
        title = "링크 (Links)",
        syntax = """
            [표시할 이름](https://example.com)
            [링크 제목과 툴팁](https://example.com "툴팁 내용")
            <https://example.com>
        """.trimIndent(),
        description = "[텍스트](URL) 형식이 기본. 홑화살괄호로 감싸면 URL 자체가 링크로 표시됩니다."
    ),
    GuideEntry(
        title = "이미지 (Images)",
        syntax = """
            ![대체 텍스트](https://example.com/image.png)
            ![로고](/path/to/local.png "이미지 툴팁")
        """.trimIndent(),
        description = "링크 앞에 !를 붙이면 이미지가 됩니다. 대체 텍스트는 로드 실패 시 표시됩니다."
    ),
    GuideEntry(
        title = "수평선 (Horizontal Rule)",
        syntax = """
            ---
            ***
            ___
        """.trimIndent(),
        description = "-, *, _ 중 하나를 3개 이상 반복해 한 줄에 두면 수평선이 그려집니다."
    ),
    GuideEntry(
        title = "표 (Tables)",
        syntax = """
            | 헤더1 | 헤더2 | 헤더3 |
            |------|:----:|-----:|
            | 왼쪽 | 가운데 | 오른쪽 |
            | 셀   | 셀    | 셀    |
        """.trimIndent(),
        description = "두 번째 줄의 :--, :-:, --: 로 각 열의 정렬을 지정합니다. (정렬 지원은 환경에 따라 다름)"
    ),
    GuideEntry(
        title = "줄바꿈 (Line Breaks)",
        syntax = """
            첫 번째 줄입니다.  ← 공백 2개 후 엔터
            두 번째 줄입니다.

            빈 줄을 두면 새 문단이 됩니다.
        """.trimIndent(),
        description = "문장 끝에 공백 2개를 두고 엔터를 치면 줄바꿈, 완전한 빈 줄을 두면 새 문단입니다."
    ),
)

@Composable
fun MarkdownGuidePanel(modifier: Modifier = Modifier, onClose: () -> Unit) {
    Column(
        modifier = modifier
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp))
            .background(GuidePanelBg, RoundedCornerShape(8.dp))
            .border(1.dp, GuideBorder, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GuideHeaderBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "마크다운 가이드",
                    color = GuideTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "편집 모드에서 아래 문법을 사용하세요",
                    color = GuideMuted,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = GuideMuted, fontSize = 16.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GuideBorder)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            guideEntries.forEach { entry ->
                GuideSection(entry)
            }

            Text(
                text = "참고: https://gist.github.com/ihoneymon/652be052a0727ad59601",
                color = GuideMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun GuideSection(entry: GuideEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            entry.title,
            color = GuideSectionTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GuideCodeBg, RoundedCornerShape(4.dp))
                .padding(10.dp)
        ) {
            Text(
                text = entry.syntax,
                fontFamily = FontFamily.Monospace,
                color = GuideCodeText,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
        Text(
            text = entry.description,
            color = GuideBody,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

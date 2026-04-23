package org.example.project

data class Memo(
    val id: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val title: String
        get() {
            val firstLine = content.lineSequence().firstOrNull() ?: ""
            return when {
                firstLine.startsWith("# ") -> firstLine.removePrefix("# ").trim()
                firstLine.isNotBlank() -> firstLine.trim().take(50)
                else -> "제목 없음"
            }
        }

    val preview: String
        get() = content.lines()
            .firstOrNull { !it.startsWith("#") && it.isNotBlank() }
            ?.trim()
            ?.take(80)
            ?: ""
}

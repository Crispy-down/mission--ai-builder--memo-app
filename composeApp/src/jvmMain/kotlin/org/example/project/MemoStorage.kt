package org.example.project

import java.io.File
import java.util.UUID

object MemoStorage {
    private val storageDir: File =
        File(System.getProperty("user.home"), ".memoapp/memos").also { it.mkdirs() }

    fun loadAll(): List<Memo> {
        return storageDir.listFiles { f -> f.extension == "md" }
            ?.map { file ->
                Memo(
                    id = file.nameWithoutExtension,
                    content = file.readText(),
                    updatedAt = file.lastModified()
                )
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    fun save(memo: Memo) {
        File(storageDir, "${memo.id}.md").writeText(memo.content)
    }

    fun delete(memo: Memo) {
        File(storageDir, "${memo.id}.md").delete()
    }

    fun createNew(): Memo {
        val memo = Memo(
            id = UUID.randomUUID().toString(),
            content = "# 새 메모\n\n내용을 입력하세요...",
            updatedAt = System.currentTimeMillis()
        )
        save(memo)
        return memo
    }
}

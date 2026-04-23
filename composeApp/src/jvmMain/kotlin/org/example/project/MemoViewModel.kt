package org.example.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MemoViewModel {
    var memos by mutableStateOf(MemoStorage.loadAll())
        private set

    var selectedMemo by mutableStateOf<Memo?>(memos.firstOrNull())
        private set

    var isPreviewMode by mutableStateOf(false)

    fun selectMemo(memo: Memo) {
        selectedMemo = memo
        isPreviewMode = false
    }

    fun createNewMemo() {
        val newMemo = MemoStorage.createNew()
        memos = listOf(newMemo) + memos
        selectMemo(newMemo)
    }

    fun updateContent(content: String) {
        val current = selectedMemo ?: return
        val updated = current.copy(content = content, updatedAt = System.currentTimeMillis())
        MemoStorage.save(updated)
        selectedMemo = updated
        memos = memos.map { if (it.id == updated.id) updated else it }
            .sortedByDescending { it.updatedAt }
    }

    fun deleteMemo(memo: Memo) {
        MemoStorage.delete(memo)
        memos = memos.filter { it.id != memo.id }
        if (selectedMemo?.id == memo.id) {
            selectedMemo = memos.firstOrNull()
        }
    }

    fun togglePreviewMode() {
        isPreviewMode = !isPreviewMode
    }

    fun clearContent() {
        if (selectedMemo != null) updateContent("")
    }
}

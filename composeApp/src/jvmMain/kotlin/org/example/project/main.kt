package org.example.project

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Good Vibe Memo",
        icon = MemoAppIcon,
        state = rememberWindowState(width = 1000.dp, height = 680.dp)
    ) {
        App()
    }
}

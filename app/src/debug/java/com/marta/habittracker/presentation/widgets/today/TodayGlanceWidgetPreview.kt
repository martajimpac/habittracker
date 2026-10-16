package com.marta.habittracker.presentation.widgets.today

import androidx.compose.runtime.Composable
import androidx.glance.action.action
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 180)
@Composable
fun TodayGlanceWidgetPreview() {
    TodayWidgetContent(
        title = "Today",
        rows = listOf(
            TodayWidgetItem(id = "1", name = "Read", completed = true),
            TodayWidgetItem(id = "2", name = "Water", completed = false),
        ),
        emptyText = "Nothing scheduled today",
        pendingFormat = "%1\$s",
        doneFormat = "Done · %1\$s",
        overflowText = null,
        openAppAction = action { },
    )
}

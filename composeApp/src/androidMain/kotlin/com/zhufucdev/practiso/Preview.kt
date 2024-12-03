package com.zhufucdev.practiso

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.page.TakeSkeleton
import com.zhufucdev.practiso.page.TakeStatCardContent

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_UNDEFINED,
    showSystemUi = false, wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE
)
@Composable
fun SessionCardPreview() {
    Column {
        TakeSkeleton(progress = 0.618f)
        TakeStatCardContent(TakeStat(
            id = 0,
            name = "test take",
            durationSeconds = 114514,
            countQuizDone = 1,
            countQuizTotal = 20,
            lastAccessTimeISO = null
        ))
    }
}

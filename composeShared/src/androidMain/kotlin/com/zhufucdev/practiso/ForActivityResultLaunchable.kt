package com.zhufucdev.practiso

import android.content.Intent
import androidx.activity.result.ActivityResult

interface ForActivityResultLaunchable {
    suspend fun startActivityForResult(intent: Intent): ActivityResult
}
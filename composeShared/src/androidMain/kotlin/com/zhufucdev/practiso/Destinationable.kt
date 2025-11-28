package com.zhufucdev.practiso

import android.app.Activity
import com.zhufucdev.practiso.platform.AppDestination

interface Destinationable {
    fun getActivity(destination: AppDestination<*>): Class<out Activity>
}
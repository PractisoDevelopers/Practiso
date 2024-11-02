package com.zhufucdev.practiso

import androidx.activity.ComponentActivity

abstract class SharedComponentActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        shared = this
    }

    companion object {
        lateinit var shared: SharedComponentActivity
            private set
    }
}
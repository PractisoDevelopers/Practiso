const val appVersion = "1.0.8-alpha"

data class AndroidSdk(val min: Int, val target: Int)
data class AndroidApp(val sdk: AndroidSdk)
val androidApp = AndroidApp(AndroidSdk(min = 24, target = 35))
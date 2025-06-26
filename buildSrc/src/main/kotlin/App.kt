const val appVersion = "1.8.6-alpha"

data class AndroidSdk(val min: Int, val target: Int)
data class AndroidApp(val sdk: AndroidSdk, val versionCode: Int)

val androidApp = AndroidApp(sdk = AndroidSdk(min = 27, target = 35), versionCode = 33)
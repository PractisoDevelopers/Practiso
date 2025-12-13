const val appVersion = "2.5.1-alpha"

data class AndroidSdk(val min: Int, val target: Int)
data class AndroidApp(val sdk: AndroidSdk, val versionCode: Int)

val androidApp = AndroidApp(sdk = AndroidSdk(min = 27, target = 36), versionCode = 43)
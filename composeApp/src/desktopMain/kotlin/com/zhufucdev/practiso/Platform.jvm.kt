package com.zhufucdev.practiso

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import kotlin.io.path.Path
import kotlin.io.path.name

abstract class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    abstract val isDarkModeEnabled: Boolean
    abstract val dataPath: String

    override suspend fun createDbDriver(): SqlDriver {
        return JdbcSqliteDriver("jdbc:sqlite:${Path(dataPath, "app.db")}")
    }
}

fun getUserHome() = System.getProperty("user.home")!!

class MacOSPlatform : JVMPlatform() {
    override val dataPath: String by lazy { Path(getUserHome(), "Library", "Application Support", "Practiso").name }

    override val isDarkModeEnabled: Boolean
        get() = MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle") == "Dark"
}

class WindowsPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = WindowsRegistry.getWindowsRegistryEntry(
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "AppsUseLightTheme"
        ) == 0x0
    override val dataPath: String by lazy { Path(System.getenv("APPDATA"), "Practiso").name }
}

class LinuxPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = Dconf.HAS_DCONF
                && Dconf.getDconfEntry("/org/gnome/desktop/interface/color-scheme").lowercase().contains("dark")

    override val dataPath: String by lazy { Path(getUserHome(), ".local", "share", "Practiso").name }
}

class OtherPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = false

    override val dataPath: String by lazy { Path(getUserHome(), ".practiso").name }
}

internal val PlatformInstance by lazy {
    System.getProperty("os.name").lowercase().let { os ->
        when {
            os.startsWith("mac") -> MacOSPlatform()
            os.startsWith("windows") -> WindowsPlatform()
            os.startsWith("linux") -> LinuxPlatform()
            else -> OtherPlatform()
        }
    }
}

actual fun getPlatform(): Platform = PlatformInstance
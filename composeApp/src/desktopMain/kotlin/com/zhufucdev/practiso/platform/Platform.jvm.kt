package com.zhufucdev.practiso.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import com.zhufucdev.practiso.database.AppDatabase
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists

abstract class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    abstract val isDarkModeEnabled: Boolean
    abstract val dataPath: String

    override fun createDbDriver(): SqlDriver {
        val emptyDb = Path(dataPath).notExists()
        if (emptyDb) {
            Path(dataPath).createDirectory()
        }
        return JdbcSqliteDriver(
            "jdbc:sqlite:${Path(dataPath, "app.db")}",
            properties = Properties().apply {
                put("foreign_keys", "true")
            }).apply {
            if (emptyDb) {
                AppDatabase.Schema.create(this)
            }
        }
    }
}

fun getUserHome() = System.getProperty("user.home")!!

class MacOSPlatform : JVMPlatform() {
    override val dataPath: String by lazy {
        Path(
            getUserHome(),
            "Library",
            "Application Support",
            "Practiso"
        ).absolutePathString()
    }

    override val isDarkModeEnabled: Boolean
        get() = MacOSDefaults.getDefaultsEntry("AppleInterfaceStyle") == "Dark"
}

class WindowsPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = WindowsRegistry.getWindowsRegistryEntry(
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "AppsUseLightTheme"
        ) == 0x0
    override val dataPath: String by lazy {
        Path(
            System.getenv("APPDATA"),
            "Practiso"
        ).absolutePathString()
    }
}

class LinuxPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = Dconf.HAS_DCONF
                && Dconf.getDconfEntry("/org/gnome/desktop/interface/color-scheme").lowercase()
            .contains("dark")

    override val dataPath: String by lazy {
        Path(
            getUserHome(),
            ".local",
            "share",
            "Practiso"
        ).absolutePathString()
    }
}

class OtherPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = false

    override val dataPath: String by lazy { Path(getUserHome(), ".practiso").absolutePathString() }
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
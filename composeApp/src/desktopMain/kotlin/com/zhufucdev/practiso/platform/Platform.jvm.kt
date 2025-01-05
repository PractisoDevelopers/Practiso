package com.zhufucdev.practiso.platform

import androidx.compose.ui.graphics.Color
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.github.tkuenneth.nativeparameterstoreaccess.Dconf
import com.github.tkuenneth.nativeparameterstoreaccess.MacOSDefaults
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.util.Properties
import java.util.prefs.Preferences
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.notExists

abstract class JVMPlatform : Platform() {
    override val name: String = "Java ${System.getProperty("java.version")}"

    abstract val isDarkModeEnabled: Boolean
    abstract val accentColor: Color?
    abstract val dataPath: String

    override fun createDbDriver(): SqlDriver {
        val dataPath = Path(dataPath)
        val dbPath = dataPath.resolve("app.db")
        if (dataPath.notExists()) {
            dataPath.createDirectory()
        }
        val dbExits = dbPath.exists()
        return JdbcSqliteDriver(
            "jdbc:sqlite:${dbPath}",
            properties = Properties().apply {
                put("foreign_keys", "true")
            }).apply {
            runBlocking {
                if (!dbExits) {
                    AppDatabase.Schema.create(this@apply).await()
                }
            }
        }
    }

    override val filesystem: FileSystem
        get() = FileSystem.SYSTEM

    override val settingsFactory: Settings.Factory by lazy {
        PreferencesSettings.Factory(Preferences.userRoot().node("/practiso"))
    }

    override val resourcePath: okio.Path by lazy {
        Path(dataPath, "resource").apply {
            if (notExists()) createDirectory()
        }.toOkioPath()
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

    override val accentColor: Color?
        get() = MacOSDefaults.getDefaultsEntry("AppleAccentColor")?.let {
            val colors = listOf(
                Color(153, 152, 152),
                Color(207, 71, 69),
                Color(232, 136, 58),
                Color(247, 201, 78),
                Color(120, 184, 86),
                Color(53, 120, 247),
                Color(139, 66, 146),
                Color(229, 92, 156)
            )
            colors[it.toInt() + 1]
        }
}

class WindowsPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = runCatching {
            WindowsRegistry.getWindowsRegistryEntry(
                """HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize""",
                "AppsUseLightTheme"
            ) == 0x0
        }.getOrDefault(false)

    override val dataPath: String by lazy {
        Path(
            System.getenv("APPDATA"),
            "Practiso"
        ).absolutePathString()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override val accentColor: Color?
        get() = runCatching {
                WindowsRegistry.getWindowsRegistryEntry(
                    """HKCU\Software\Microsoft\Windows\DWM""",
                    "AccentColor",
                    WindowsRegistry.REG_TYPE.REG_DWORD
                ).hexToInt(HexFormat { number.prefix = "0x" })
            }.getOrNull()?.let {
            Color(
                alpha = it shr (24) and (0xFF),
                blue = it shr (16) and (0xFF),
                green = it shr (8) and (0xFF),
                red = it and 0xFF
            )
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

    override val accentColor: Color?
        get() = null
}

class OtherPlatform : JVMPlatform() {
    override val isDarkModeEnabled: Boolean
        get() = false

    override val accentColor: Color?
        get() = null

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
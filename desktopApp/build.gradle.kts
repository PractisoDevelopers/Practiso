import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

compose.desktop {
    application {
        mainClass = "com.zhufucdev.practiso.MainKt"

        nativeDistributions {
            packageName = rootProject.name
            packageVersion =
                appVersion
                    .takeIf { it.endsWith("-alpha") }
                    ?.let { it.substring(0, it.indexOf('-')) }
                    ?: ""
            fileAssociation(
                mimeType = "application/gzip",
                extension = "psarchive",
                description = "Practiso Archive"
            )

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules("java.sql")

            windows {
                upgradeUuid = "0744E069-28F6-42BD-97B5-E99E02001A53"
                menu = true
            }
            linux {
                modules("jdk.security.auth")
            }
            macOS {
                jvmArgs("-Dapple.awt.application.appearance=system")
            }
        }

        buildTypes.release.proguard {
            version.set("7.6.1")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.materialkolor)
}

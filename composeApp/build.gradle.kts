@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.skie)
}

val appVersion = "1.0.5-alpha"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().forEach {
        it.binaries.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>()
            .forEach { lib ->
                lib.isStatic = false
                lib.linkerOpts.add("-lsqlite3")
            }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation)
            implementation(libs.material3.adaptative)
            implementation(libs.material3.windowsize)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.filekit)
            implementation(libs.okio)
            implementation(libs.humanreadable)
            implementation(libs.settings.core)
            implementation(libs.settings.coroutine)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio.fakefs)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.jvm.driver)
            implementation(libs.nativeparameteraccess)
            implementation(libs.materialkolor)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "com.zhufucdev.practiso"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    defaultConfig {
        applicationId = "com.zhufucdev.practiso"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 6
        versionName = appVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules-android.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
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
                    ?: appVersion
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
            configurationFiles.from(project.file("proguard-rules-desktop.pro"))
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName = "com.zhufucdev.practiso.database"
            generateAsync = true
        }
    }
    linkSqlite = true
}

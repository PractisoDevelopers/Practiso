@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.skie)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
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
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            api(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            api(libs.androidx.navigation)
            implementation(libs.material3.adaptative)
            implementation(libs.material3.windowsize)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)
            api(libs.filekit)
            api(libs.okio)
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
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.jvm.driver)
            implementation(libs.nativeparameteraccess)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
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

android {
    namespace = "com.zhufucdev.practiso"
    compileSdk = androidApp.sdk.target
    defaultConfig {
        minSdk = androidApp.sdk.min
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.resources {
    generateResClass = always
    packageOfResClass = "resources"
}
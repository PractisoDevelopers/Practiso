@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinxSerialization)
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
        iosSimulatorArm64(),
        macosArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().forEach {
        it.binaries.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>()
            .forEach { lib ->
                lib.isStatic = false
                lib.linkerOpts.add("-lsqlite3")
            }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.sqldelight.runtime)
            api(libs.sqldelight.coroutines)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.serialization.protobuf)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.io.okio)
            api(libs.filekit.core)
            api(libs.okio)
            implementation(libs.settings.core)
            implementation(libs.settings.coroutine)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio.fakefs)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.onnx.runtime.android)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sqldelight.jvm.driver)
                implementation(libs.nativeparameteraccess)
                implementation(libs.onnx.runtime.jvm)
            }
        }

        appleMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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

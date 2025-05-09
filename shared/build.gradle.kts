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
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64()
    ).forEach { nativeTarget ->
        nativeTarget.binaries {
            framework {
                baseName = "ComposeApp"
                isStatic = false
                linkerOpts += "-lsqlite3"
            }
            sharedLib()
            staticLib()
        }
    }

    applyDefaultHierarchyTemplate()

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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.settings.core)
            implementation(libs.settings.coroutine)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.hgtk.core)
            implementation(libs.usearch.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio.fakefs)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.mlkit.langid)
            implementation(libs.litert)
            implementation(libs.litert.gpu)
            implementation(libs.litert.support)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sqldelight.jvm.driver)
                implementation(libs.nativeparameteraccess)
                implementation(libs.onnx.runtime.jvm)
                implementation(libs.lingua)
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

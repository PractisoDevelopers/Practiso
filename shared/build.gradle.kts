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

    applyDefaultHierarchyTemplate {
        group("composeCommon") {
            withAndroidTarget()
            withJvm()
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.hgtk.core)
            implementation(libs.usearch.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.serialization.json)
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
            implementation(libs.androidx.lifecycle.service)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.tink.android)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.bundles.androidx.test)
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sqldelight.jvm.driver)
                implementation(libs.nativeparameteraccess)
                implementation(libs.onnx.runtime.jvm)
                implementation(libs.lingua)
                implementation(libs.ktor.client.okhttp)
            }
        }

        appleMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }

        val composeCommonMain by getting {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.settings.core)
                implementation(libs.settings.coroutine)
            }
        }

        val composeCommonTest by getting {
            dependsOn(commonTest.get())
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

skie {
    features {
        enableSwiftUIObservingPreview = true
    }
}
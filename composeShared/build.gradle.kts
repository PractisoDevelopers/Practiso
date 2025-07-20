import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.components.resources)
            api(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.materialicons.core)
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            api(libs.androidx.navigation)
            implementation(libs.material3.adaptative)
            implementation(libs.material3.windowsize)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.humanreadable)
            implementation(libs.settings.core)
            implementation(libs.settings.coroutine)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.resources {
    packageOfResClass = "resources"
    generateResClass = auto
}

android {
    namespace = "com.zhufucdev.practiso"
    compileSdk = androidApp.sdk.target
    buildFeatures {
        compose = true
    }
    defaultConfig {
        minSdk = androidApp.sdk.min
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

# Practiso

[![Build and Release](https://github.com/zhufucdev/Practiso/actions/workflows/compose-app.yml/badge.svg)](https://github.com/zhufucdev/Practiso/actions/workflows/compose-app.yml)
[![GitHub Tag](https://img.shields.io/github/v/tag/zhufucdev/Practiso)](https://github.com/zhufucdev/Practiso/tags)
[![GitHub Release](https://img.shields.io/github/v/release/zhufucdev/Practiso)](https://github.com/zhufucdev/Practiso/releases)

Personal, offline and intelligent study & practice utility. Learn smarter, not more.

<p align="center"><img src="desktopApp/icons/icon.png" alt="Practiso logo" width="200px" /></p>

## Screenshots

| Name                |                      Screenshot                      |
|:--------------------|:----------------------------------------------------:|
| Home                |        ![home-screen](assets/home-screen.png)        |
| Home iOS            |      ![home-screen](assets/home-screen-ios.png)      |
| Quiz editor 1       |      ![quiz-editor-1](assets/quiz-editor-1.png)      |
| Quiz editor 2       |      ![quiz-editor-2](assets/quiz-editor-2.png)      |
| Quiz editor iOS     |    ![quiz-editor-ios](assets/quiz-editor-ios.png)    |
| Answer screen       |      ![answer-screen](assets/answer-screen.png)      |
| Answer screen iOS   |  ![answer-screen-ios](assets/answer-screen-ios.png)  |                 
| Take details dialog | ![take-details-card](assets/take-details-dialog.png) |

## Project Structure

This is a Kotlin Multiplatform project targeting Android, iOS, Desktop.
Android and desktop platforms utilize Jetpack Compose framework, while on iOS
SwiftUI is in play.

* `/shared` is for code that will be shared across the multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the
      folder name.
      For example, `appleMain` for Apple platform specifications.

While the above contribute to most of Practiso business logic, the following code brings
user experience to life.

* `/composeShared` is dependency of `/androidApp` and `/desktopApp`, containing subfolders for
  platform specific behaviors from how secondary click triggers to state management and navigation.

* `/iosApp` contains iOS applications, written in Swift. It interacts with Kotlin world through
  its ObjC interoperability, which compiles the `ComposeApp` Xcode framework.

Learn more
about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

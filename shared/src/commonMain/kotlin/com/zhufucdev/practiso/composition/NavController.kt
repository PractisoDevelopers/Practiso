package com.zhufucdev.practiso.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController = staticCompositionLocalOf<NavHostController?> { null }

@Composable
fun currentNavController() = LocalNavController.current ?: error("No current NavController provided")
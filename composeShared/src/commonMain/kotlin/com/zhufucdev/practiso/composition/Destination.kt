package com.zhufucdev.practiso.composition

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import resources.Res
import resources.baseline_earth
import resources.baseline_library_books
import resources.community_para
import resources.library_para
import resources.session_para

enum class TopLevelDestination(
    val nameRes: StringResource,
    val icon: @Composable () -> Unit,
    val route: String,
) {
    Session(
        nameRes = Res.string.session_para,
        icon = { Icon(Icons.Default.Star, "") },
        route = "session",
    ),
    Library(
        nameRes = Res.string.library_para,
        icon = { Icon(painterResource(Res.drawable.baseline_library_books), "") },
        route = "library",
    ),
    Community(
        nameRes = Res.string.community_para,
        icon = { Icon(painterResource(Res.drawable.baseline_earth), "") },
        route = "community"
    )
}

val LocalTopLevelDestination = compositionLocalOf<TopLevelDestination?> { null }

@Composable
fun currentTopLevelDestination(): TopLevelDestination {
    return LocalTopLevelDestination.current ?: error("Top Level Destination not set in context.")
}
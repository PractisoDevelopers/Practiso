package com.zhufucdev.practiso

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.page.LibraryApp
import com.zhufucdev.practiso.page.SessionApp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.app_name
import practiso.composeapp.generated.resources.baseline_library_books
import practiso.composeapp.generated.resources.library_para
import practiso.composeapp.generated.resources.session_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val topBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.app_name)) },
                scrollBehavior = topBarBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach {
                    NavigationBarItem(
                        selected = navBackStackEntry?.destination?.route == it.route,
                        onClick = { navController.navigate(it.route) },
                        icon = it.icon,
                        label = { Text(stringResource(it.nameRes)) },
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            CompositionLocalProvider(
                LocalNavController provides navController,
            ) {
                NavigatedApp()
            }
        }
    }
}

private enum class TopLevelDestination(
    val nameRes: StringResource,
    val icon: @Composable () -> Unit,
    val route: String,
) {
    Session(
        nameRes = Res.string.session_para,
        icon = { Icon(Icons.Default.Star, "") },
        route = "session"
    ),
    Library(
        nameRes = Res.string.library_para,
        icon = { Icon(painterResource(Res.drawable.baseline_library_books), "") },
        route = "library"
    ),
}

@Composable
private fun NavigatedApp() {
    NavHost(
        navController = currentNavController(),
        startDestination = TopLevelDestination.Session.route,
    ) {
        composable(TopLevelDestination.Session.route) {
            SessionApp()
        }
        composable(TopLevelDestination.Library.route) {
            LibraryApp()
        }
    }
}
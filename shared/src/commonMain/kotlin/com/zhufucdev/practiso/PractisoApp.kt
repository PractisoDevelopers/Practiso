package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowWidthSizeClass
import com.zhufucdev.practiso.composable.BackHandlerOrIgnored
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.ImportDialog
import com.zhufucdev.practiso.composable.ImportState
import com.zhufucdev.practiso.composable.PractisoOptionView
import com.zhufucdev.practiso.composable.SharedElementTransitionKey
import com.zhufucdev.practiso.composition.BottomUpComposableScope
import com.zhufucdev.practiso.composition.LocalBottomUpComposable
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.page.LibraryApp
import com.zhufucdev.practiso.page.SessionApp
import com.zhufucdev.practiso.page.SessionStarter
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import com.zhufucdev.practiso.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_library_books
import resources.deactivate_global_search_span
import resources.library_para
import resources.search_app_para
import resources.session_para
import kotlin.reflect.typeOf

@Composable
fun PractisoApp(
    navController: NavHostController,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
    importViewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()

    BottomUpComposableScope { buc ->
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalBottomUpComposable provides buc
        ) {
            when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
                WindowWidthSizeClass.COMPACT ->
                    ScaffoldedApp(
                        importViewModel,
                        searchViewModel,
                        windowAdaptiveInfo,
                        navController
                    )

                WindowWidthSizeClass.MEDIUM -> Row {
                    NavigationRail {
                        Spacer(Modifier.padding(top = PaddingNormal))
                        TopLevelDestination.entries.forEach {
                            NavigationRailItem(
                                selected = navBackStackEntry?.destination?.let { d -> it.isCurrent(d) } == true,
                                onClick = {
                                    if (navBackStackEntry?.destination?.route != it.route) {
                                        navController.navigate(it.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = it.icon,
                                label = { Text(stringResource(it.nameRes)) },
                            )
                        }
                    }
                    ScaffoldedApp(
                        importViewModel,
                        searchViewModel,
                        windowAdaptiveInfo,
                        navController
                    )
                }

                WindowWidthSizeClass.EXPANDED -> Row {
                    PermanentDrawerSheet {
                        Spacer(Modifier.padding(top = PaddingNormal))
                        TopLevelDestination.entries.forEach {
                            NavigationDrawerItem(
                                selected = navBackStackEntry?.destination?.let { d -> it.isCurrent(d) } == true,
                                onClick = {
                                    if (navBackStackEntry?.destination?.route != it.route) {
                                        navController.navigate(it.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = it.icon,
                                label = { Text(stringResource(it.nameRes)) },
                            )
                        }
                    }
                    ScaffoldedApp(
                        importViewModel,
                        searchViewModel,
                        windowAdaptiveInfo,
                        navController
                    )
                }
            }

            buc.compose(SharedElementTransitionKey)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldedApp(
    importViewModel: ImportViewModel,
    searchViewModel: SearchViewModel,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val topBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val buc = LocalBottomUpComposable.current

    Scaffold(
        topBar = {
            TopSearchBar(searchViewModel) {
                navController.navigate(
                    LibraryAppViewModel.Revealable(
                        id = it.id,
                        type =
                            when (it) {
                                is PractisoOption.Dimension -> LibraryAppViewModel.RevealableType.Dimension
                                is PractisoOption.Quiz -> LibraryAppViewModel.RevealableType.Quiz
                                else -> error("Unsupported revealing type: ${it::class.simpleName}")
                            }
                    )
                )
            }
        },
        bottomBar = {
            when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
                WindowWidthSizeClass.COMPACT -> {
                    NavigationBar {
                        TopLevelDestination.entries.forEach {
                            NavigationBarItem(
                                selected = navBackStackEntry?.destination?.let { d -> it.isCurrent(d) } == true,
                                onClick = {
                                    if (navBackStackEntry?.destination?.route != it.route) {
                                        navController.navigate(it.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = it.icon,
                                label = { Text(stringResource(it.nameRes)) },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedContent(
                buc?.get("fab"),
                contentAlignment = Alignment.BottomEnd,
                transitionSpec = {
                    scaleIn().togetherWith(scaleOut())
                }
            ) { content ->
                content?.invoke()
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            NavigatedApp(importViewModel)
        }
    }

    val state by importViewModel.state.collectAsState()
    if (state != ImportState.Idle) {
        ImportDialog(state)
    }
}

internal enum class TopLevelDestination(
    val nameRes: StringResource,
    val icon: @Composable () -> Unit,
    val route: String,
    val isCurrent: (NavDestination) -> Boolean,
) {
    Session(
        nameRes = Res.string.session_para,
        icon = { Icon(Icons.Default.Star, "") },
        route = "session",
        isCurrent = {
            it.route?.startsWith("session") == true
        }
    ),
    Library(
        nameRes = Res.string.library_para,
        icon = { Icon(painterResource(Res.drawable.baseline_library_books), "") },
        route = "library",
        isCurrent = {
            it.route?.startsWith("library") == true || it.hasRoute(LibraryAppViewModel.Revealable::class)
        }
    ),
}

@Composable
private fun NavigatedApp(importer: ImportViewModel) {
    NavHost(
        navController = currentNavController(),
        startDestination = TopLevelDestination.Session.route,
    ) {
        composable(TopLevelDestination.Session.route) {
            SessionApp()
        }
        composable(TopLevelDestination.Library.route) {
            LibraryApp(importer = importer)
        }
        composable<LibraryAppViewModel.Revealable>(
            typeMap = mapOf(
                typeOf<LibraryAppViewModel.Revealable>() to LibraryAppViewModel.RevealableNavType,
                typeOf<LibraryAppViewModel.RevealableType>() to LibraryAppViewModel.RevealableTypeNavType
            )
        ) { backtrace ->
            val model: LibraryAppViewModel = viewModel(factory = LibraryAppViewModel.Factory)
            LaunchedEffect(backtrace) {
                model.event.reveal.send(backtrace.toRoute())
            }
            LibraryApp(model, importer)
        }
        composable("${TopLevelDestination.Session.route}/new") {
            SessionStarter()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopSearchBar(model: SearchViewModel, onSearchResultClick: (PractisoOption) -> Unit) {
    val query by model.query.collectAsState()
    val active by model.active.collectAsState()

    val padding = animateFloatAsState(if (active) 0f else PaddingNormal.value)
    val coroutine = rememberCoroutineScope()

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onSearch = {},
                onQueryChange = {
                    coroutine.launch {
                        model.event.updateQuery.send(it)
                    }
                },
                expanded = active,
                onExpandedChange = { expand ->
                    coroutine.launch {
                        if (expand) {
                            model.event.open.send(Unit)
                        } else {
                            model.event.close.send(Unit)
                        }
                    }
                },
                leadingIcon = {
                    AnimatedContent(active) { active ->
                        if (!active) {
                            Icon(Icons.Default.Search, "")
                        } else {
                            IconButton(
                                onClick = {
                                    coroutine.launch {
                                        model.event.close.send(Unit)
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    stringResource(Res.string.deactivate_global_search_span)
                                )
                            }
                        }
                    }
                },
                placeholder = {
                    Text(stringResource(Res.string.search_app_para))
                },
            )
        },
        expanded = active,
        onExpandedChange = { expand ->
            coroutine.launch {
                if (expand) {
                    model.event.open.send(Unit)
                } else {
                    model.event.close.send(Unit)
                }
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = padding.value.dp)
    ) {
        BackHandlerOrIgnored {
            coroutine.launch {
                model.event.close.send(Unit)
            }
        }

        val options by model.result.collectAsState()
        val searching by model.searching.collectAsState()
        AnimatedVisibility(visible = searching, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        val listState = rememberLazyListState()
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(listState.lastScrolledForward) {
            if (listState.lastScrolledForward) {
                keyboard?.hide()
            }
        }

        LazyColumn(state = listState) {
            items(
                count = options.size,
                key = { i -> options[i]::class.simpleName!! + options[i].id }
            ) { index ->
                val option = options[index]

                Box(Modifier.fillMaxWidth().animateItem().clickable {
                    coroutine.launch {
                        model.event.close.send(Unit)
                    }
                    onSearchResultClick(option)
                }) {
                    PractisoOptionView(option, modifier = Modifier.padding(PaddingNormal))
                }

                if (index < options.lastIndex) {
                    Box(Modifier.padding(start = PaddingNormal)) {
                        HorizontalSeparator()
                    }
                }
            }
        }
    }
}
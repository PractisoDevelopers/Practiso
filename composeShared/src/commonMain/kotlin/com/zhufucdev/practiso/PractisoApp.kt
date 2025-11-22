@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.zhufucdev.practiso.composable.BackHandlerOrIgnored
import com.zhufucdev.practiso.composable.BackdropKey
import com.zhufucdev.practiso.composable.ExtensiveSnackbar
import com.zhufucdev.practiso.composable.FeiStatus
import com.zhufucdev.practiso.composable.HorizontalSeparator
import com.zhufucdev.practiso.composable.ImportDialog
import com.zhufucdev.practiso.composable.NavigateUpButton
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.PractisoOptionView
import com.zhufucdev.practiso.composable.SharedElementTransitionKey
import com.zhufucdev.practiso.composition.BottomUpComposableScope
import com.zhufucdev.practiso.composition.LocalBottomUpComposable
import com.zhufucdev.practiso.composition.LocalExtensiveSnackbarState
import com.zhufucdev.practiso.composition.LocalNavController
import com.zhufucdev.practiso.composition.LocalTopLevelDestination
import com.zhufucdev.practiso.composition.TopLevelDestination
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.composition.currentTopLevelDestination
import com.zhufucdev.practiso.composition.rememberExtensiveSnackbarState
import com.zhufucdev.practiso.datamodel.DimensionOption
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.page.CommunityApp
import com.zhufucdev.practiso.page.CommunityArchiveApp
import com.zhufucdev.practiso.page.CommunityDimensionApp
import com.zhufucdev.practiso.page.DimensionApp
import com.zhufucdev.practiso.page.DimensionSectionEditApp
import com.zhufucdev.practiso.page.LibraryApp
import com.zhufucdev.practiso.page.QuizSectionEditApp
import com.zhufucdev.practiso.page.SessionApp
import com.zhufucdev.practiso.page.SessionStarter
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.route.ArchiveMetadataNavType
import com.zhufucdev.practiso.route.ArchivePreviewRouteParams
import com.zhufucdev.practiso.route.CommunityDimensionRouteParams
import com.zhufucdev.practiso.route.DimensionAppRouteParams
import com.zhufucdev.practiso.service.ImportState
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.ArchiveSharingViewModel
import com.zhufucdev.practiso.viewmodel.CommunityAppViewModel
import com.zhufucdev.practiso.viewmodel.CommunityArchiveViewModel
import com.zhufucdev.practiso.viewmodel.CommunityDimensionViewModel
import com.zhufucdev.practiso.viewmodel.DimensionSectionEditVM
import com.zhufucdev.practiso.viewmodel.ImportViewModel
import com.zhufucdev.practiso.viewmodel.LibraryAppViewModel
import com.zhufucdev.practiso.viewmodel.QuizSectionEditVM
import com.zhufucdev.practiso.viewmodel.SearchViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import opacity.client.ArchiveMetadata
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.deactivate_global_search_span
import resources.search_app_para
import resources.settings_para
import resources.show_more_para
import kotlin.reflect.typeOf

@Composable
fun PractisoApp(navController: NavHostController) {
    val libraryVM: LibraryAppViewModel =
        viewModel(factory = LibraryAppViewModel.Factory)
    val searchVM: SearchViewModel =
        viewModel(factory = SearchViewModel.Factory)
    val archiveSharingVM: ArchiveSharingViewModel =
        viewModel(factory = ArchiveSharingViewModel.Factory)
    val importVM: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
    val communityVM: CommunityAppViewModel = viewModel(factory = CommunityAppViewModel.Factory)

    BottomUpComposableScope {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = TopLevelDestination.Session.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { fadeOut() }
            ) {
                composable(TopLevelDestination.Session.route) {
                    AdaptiveApp(navController, TopLevelDestination.Session, searchVM) {
                        ScaffoldedApp(it, searchVM) {
                            SessionApp()
                        }
                    }
                }
                composable(TopLevelDestination.Library.route) {
                    LaunchedEffect(libraryVM) {
                        libraryVM.event.removeReveal.send(Unit)
                    }
                    AdaptiveApp(navController, TopLevelDestination.Library, searchVM) {
                        ScaffoldedApp(it, searchVM) {
                            LibraryApp(model = libraryVM, importer = importVM)
                        }
                    }
                }
                composable(TopLevelDestination.Community.route) {
                    AdaptiveApp(navController, TopLevelDestination.Community, searchVM) {
                        ScaffoldedApp(it, searchVM) {
                            CommunityApp(
                                communityVM = communityVM,
                                importVM = importVM,
                                sharedTransition = this@SharedTransitionLayout,
                                animatedContent = this@composable
                            )
                        }
                    }
                }
                composable<LibraryAppViewModel.Revealable>(
                    typeMap = mapOf(
                        typeOf<LibraryAppViewModel.Revealable>() to LibraryAppViewModel.RevealableNavType,
                        typeOf<LibraryAppViewModel.RevealableType>() to LibraryAppViewModel.RevealableTypeNavType
                    )
                ) { backtrace ->
                    AdaptiveApp(navController, TopLevelDestination.Library, searchVM) {
                        ScaffoldedApp(it, searchVM) {
                            LaunchedEffect(backtrace) {
                                libraryVM.event.reveal.send(backtrace.toRoute())
                            }
                            LibraryApp(
                                model = libraryVM,
                                importer = importVM
                            )
                        }
                    }
                }
                composable("${TopLevelDestination.Session.route}/new") {
                    AdaptiveApp(navController, TopLevelDestination.Session, searchVM) {
                        ScaffoldedApp(it, searchVM) {
                            SessionStarter()
                        }
                    }
                }
                composable<DimensionSectionEditVM.Startpoint> { stackEntry ->
                    AdaptiveApp(navController, TopLevelDestination.Library, searchVM) {
                        DimensionSectionEditApp(
                            startpoint = stackEntry.toRoute(),
                            libraryVm = libraryVM,
                            archiveSharingVM = archiveSharingVM
                        )
                    }
                }
                composable<QuizSectionEditVM.Startpoint> { stackEntry ->
                    AdaptiveApp(navController, TopLevelDestination.Library, searchVM) {
                        QuizSectionEditApp(
                            startpoint = stackEntry.toRoute(),
                            libraryVM = libraryVM,
                            archiveSharingVM = archiveSharingVM
                        )
                    }
                }
                composable<DimensionAppRouteParams> { stackEntry ->
                    AdaptiveApp(navController, TopLevelDestination.Library, searchVM) {
                        ScaffoldedApp(it, searchVM) {
                            DimensionApp(stackEntry.toRoute())
                        }
                    }
                }
                composable<ArchivePreviewRouteParams>(
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
                    typeMap = mapOf(
                        typeOf<ArchiveMetadata>() to ArchiveMetadataNavType
                    )
                ) { stackEntry ->
                    val vm: CommunityArchiveViewModel =
                        viewModel(factory = CommunityArchiveViewModel.Factory)
                    LaunchedEffect(vm, stackEntry) {
                        vm.loadParameters(stackEntry.toRoute())
                    }
                    CompositionLocalProvider(
                        LocalNavController provides navController
                    ) {
                        CommunityArchiveApp(
                            previewVM = vm,
                            importVM = importVM,
                            communityVM = communityVM,
                            sharedTransition = this@SharedTransitionLayout,
                            animatedContent = this@composable
                        )
                    }
                }
                composable<CommunityDimensionRouteParams>(
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) { stackEntry ->
                    AdaptiveApp(
                        navController = navController,
                        destination = TopLevelDestination.Community,
                        searchViewModel = searchVM
                    ) { window ->
                        val vm: CommunityDimensionViewModel =
                            viewModel(factory = CommunityDimensionViewModel.Factory)
                        LaunchedEffect(vm, stackEntry) {
                            vm.loadRouteParams(stackEntry.toRoute())
                        }
                        val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                        ScaffoldedApp(
                            windowAdaptiveInfo = window,
                            searchViewModel = searchVM,
                            topBar = {
                                TopAppBar(
                                    title = {
                                        val dimensionName by vm.dimension.collectAsState()
                                        Text(dimensionName)
                                    },
                                    navigationIcon = {
                                        NavigateUpButton {
                                            navController.popBackStack()
                                        }
                                    },
                                    scrollBehavior = topBarScrollBehavior
                                )
                            }
                        ) {
                            CommunityDimensionApp(
                                dimensionModel = vm,
                                importModel = importVM,
                                scrollConnection = topBarScrollBehavior.nestedScrollConnection,
                                sharedTransition = this@SharedTransitionLayout,
                                animatedContent = this
                            )
                        }
                    }
                }
            }
        }
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            LocalBottomUpComposable.current!!.compose(SharedElementTransitionKey)
        }
    }

    val importState by importVM.state.collectAsState()
    if (importState !is ImportState.Idle) {
        ImportDialog(importState)
    }
}

private const val BOTTOM_NAVIGATION_BREAKPOINT = 600
private const val SIDE_NAVIGATION_BREAKPOINT = 840

@Composable
private fun AdaptiveApp(
    navController: NavHostController,
    destination: TopLevelDestination,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
    content: @Composable (WindowAdaptiveInfo) -> Unit,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalTopLevelDestination provides destination
    ) {
        when (windowAdaptiveInfo.windowSizeClass.minWidthDp) {
            in 0 until BOTTOM_NAVIGATION_BREAKPOINT -> content(windowAdaptiveInfo)

            in BOTTOM_NAVIGATION_BREAKPOINT until SIDE_NAVIGATION_BREAKPOINT -> Row {
                val coroutine = rememberCoroutineScope()
                NavigationRail {
                    Spacer(Modifier.padding(top = PaddingNormal))
                    TopLevelDestination.entries.forEach {
                        NavigationRailItem(
                            selected = it == destination,
                            onClick = {
                                coroutine.launch {
                                    searchViewModel.event.close.send(Unit)
                                }
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
                content(windowAdaptiveInfo)
            }

            else -> Row {
                PermanentDrawerSheet {
                    val coroutine = rememberCoroutineScope()
                    Spacer(Modifier.padding(top = PaddingNormal))
                    TopLevelDestination.entries.forEach {
                        NavigationDrawerItem(
                            selected = it == destination,
                            onClick = {
                                coroutine.launch {
                                    searchViewModel.event.close.send(Unit)
                                }
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
                content(windowAdaptiveInfo)
            }
        }
    }
}

@Composable
private fun ScaffoldedApp(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
    topBar: @Composable () -> Unit = { ScaffoldedApp_TopSearchBar(model = searchViewModel) },
    content: @Composable () -> Unit,
) {
    val navController = currentNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val buc = LocalBottomUpComposable.current
    val snackbars = rememberExtensiveSnackbarState()

    CompositionLocalProvider(
        LocalExtensiveSnackbarState provides snackbars
    ) {
        Scaffold(
            topBar = topBar,
            bottomBar = {
                if (!windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                        BOTTOM_NAVIGATION_BREAKPOINT
                    )
                ) {
                    val coroutine = rememberCoroutineScope()
                    ScaffoldedApp_NavigationBar(
                        onClickDestination = {
                            coroutine.launch {
                                searchViewModel.event.close.send(Unit)
                            }
                            if (navBackStackEntry?.destination?.route != it.route) {
                                navController.navigate(it.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
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
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbars.host) {
                    ExtensiveSnackbar(state = snackbars, data = it)
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                content()
            }
            buc?.compose(BackdropKey)
        }

        val feiState by Database.fei.getUpgradeState().collectAsState(null)
        feiState?.let { FeiStatus(it) }
    }
}

@Composable
private fun ScaffoldedApp_NavigationBar(
    onClickDestination: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        val destination = currentTopLevelDestination()
        TopLevelDestination.entries.forEach {
            NavigationBarItem(
                selected = destination == it,
                onClick = {
                    onClickDestination(it)
                },
                icon = it.icon,
                label = { Text(stringResource(it.nameRes)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldedApp_TopSearchBar(
    modifier: Modifier = Modifier,
    model: SearchViewModel,
) {
    val query by model.query.collectAsState()
    val active by model.active.collectAsState()
    val expanded by model.expanded.collectAsState()

    val padding = animateFloatAsState(if (expanded) 0f else PaddingNormal.value)
    val coroutine = rememberCoroutineScope()
    val navController = currentNavController()

    if (active) {
        BackHandlerOrIgnored { event ->
            model.event.hide.send(Unit)
            try {
                event.collect {
                    if (it.progress > 0.1) {
                        model.event.hide.send(Unit)
                    } else if (it.progress <= 0) {
                        model.event.open.send(Unit)
                    }
                }
                model.event.close.send(Unit)
            } catch (e: CancellationException) {
                model.event.open.send(Unit)
            }
        }
    }

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
                expanded = expanded,
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
                    AnimatedContent(expanded) { expanded ->
                        if (!expanded) {
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
                trailingIcon = {
                    AnimatedContent(expanded) { expanded ->
                        if (!expanded) {
                            Box {
                                var menuOpen by remember { mutableStateOf(false) }
                                PlainTooltipBox(stringResource(Res.string.show_more_para)) {
                                    IconButton(onClick = {
                                        menuOpen = true
                                    }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(Res.string.settings_para))
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Settings, contentDescription = null)
                                        },
                                        onClick = {
                                            menuOpen = false
                                            coroutine.launch {
                                                Navigator.navigate(Navigation.Goto(AppDestination.Preferences))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                placeholder = {
                    Text(stringResource(Res.string.search_app_para))
                },
            )
        },
        expanded = expanded,
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
                    then modifier
    ) {
        val options by model.result.collectAsState(emptyList(), Dispatchers.IO)
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
                    navController.navigate(
                        LibraryAppViewModel.Revealable(
                            id = option.id,
                            type =
                                when (option) {
                                    is DimensionOption -> LibraryAppViewModel.RevealableType.Dimension
                                    is QuizOption -> LibraryAppViewModel.RevealableType.Quiz
                                    else -> error("Unsupported revealing type: ${option::class.simpleName}")
                                }
                        )
                    )
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
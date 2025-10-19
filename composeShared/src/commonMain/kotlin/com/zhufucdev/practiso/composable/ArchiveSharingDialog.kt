package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.SavedState
import com.zhufucdev.practiso.AppSettings
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.composition.pseudoClickable
import com.zhufucdev.practiso.helper.protobufSaver
import com.zhufucdev.practiso.route.ArchivePreviewRouteParams
import com.zhufucdev.practiso.service.CommunityRegistrationInfo
import com.zhufucdev.practiso.service.UploadArchive
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.ArchiveSharingViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.action_was_cancelled_para
import resources.baseline_archive_arrow_down_outline
import resources.baseline_check_circle_outline
import resources.cancel_para
import resources.clear_token_para
import resources.continue_in_system_file_manager_para
import resources.device_name_para
import resources.export_to_file_para
import resources.from_server_x_para
import resources.http_error_para
import resources.must_reregister_to_proceed_para
import resources.name_of_entry_para
import resources.outline_alert_circle
import resources.outline_cloud_upload
import resources.owner_name_para
import resources.registration_required_para
import resources.retry_para
import resources.reveal_para
import resources.shared_with_the_community_para
import resources.sharing_n_items_para
import resources.upload_para
import resources.upload_to_community_para
import resources.waiting_for_service_para

@Composable
expect fun ArchiveSharingDialog(
    modifier: Modifier = Modifier,
    model: ArchiveSharingViewModel,
    onDismissRequested: () -> Unit,
)

@Composable
fun ArchiveSharingDialogScaffold(
    modifier: Modifier = Modifier,
    model: ArchiveSharingViewModel,
    onDismissRequested: () -> Unit,
    builder: ArchiveSharingDialogBuilder.() -> Unit,
) {
    Card(Modifier.safeContentPadding().pseudoClickable() then modifier) {
        val navController = rememberNavController()
        var starterOptions by remember(builder) { mutableStateOf<List<StarterPageOption>>(emptyList()) }
        val pageNavStack = remember(builder) { mutableStateListOf<PageNavStackEntry>() }

        DisposableEffect(navController) {
            var stackSize = navController.currentBackStack.value.size
            val listener = object : NavController.OnDestinationChangedListener {
                override fun onDestinationChanged(
                    controller: NavController,
                    destination: NavDestination,
                    arguments: SavedState?,
                ) {
                    val newStackSize = navController.currentBackStack.value.size
                    if (newStackSize < stackSize) {
                        // popped
                        if (pageNavStack.isNotEmpty()) {
                            pageNavStack.removeAt(pageNavStack.size - 1)
                        }
                    }
                    stackSize = newStackSize
                }
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose {
                navController.removeOnDestinationChangedListener(listener)
            }
        }

        TopAppBar(
            title = {
                pageNavStack
                    .lastOrNull()
                    ?.decorations
                    ?.topBarTitle
                    ?.invoke()
                    ?: Text(
                        pluralStringResource(
                            Res.plurals.sharing_n_items_para,
                            model.selection.size,
                            model.selection.size
                        )
                    )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                val topEntry by navController.currentBackStackEntryAsState()
                if (topEntry?.destination?.route == "starter") {
                    return@TopAppBar
                }
                NavigateUpButton { navController.navigateUp() }
            },
            windowInsets = WindowInsets()
        )
        NavHost(
            navController = navController,
            startDestination = "starter",
            enterTransition = {
                fadeIn() + slideInHorizontally(initialOffsetX = {
                    if (isNavigateForward(navController))
                        it / 2
                    else
                        -it / 2
                })
            },
            exitTransition = {
                fadeOut() + slideOutHorizontally(targetOffsetX = {
                    if (isNavigateForward(navController))
                        -it / 2
                    else
                        it / 2
                })
            },
            sizeTransform = { SizeTransform() }
        ) {
            var latestBuildId = 0

            fun composable(
                routeName: String,
                content: @Composable ArchiveSharingPageScope.(NavBackStackEntry) -> Unit,
            ) {
                this@NavHost.composable(route = routeName) { entry ->
                    val positionInStack = remember {
                        pageNavStack.indexOfLast { it.routeName == routeName }
                            .takeIf { it >= 0 } ?: pageNavStack.size
                    }

                    LaunchedEffect(routeName) {
                        if (pageNavStack.size <= positionInStack) {
                            pageNavStack.add(PageNavStackEntry(routeName, ownerId = latestBuildId))
                        } else if (pageNavStack[positionInStack].routeName != routeName) {
                            pageNavStack[positionInStack] =
                                PageNavStackEntry(routeName, ownerId = latestBuildId)
                        }
                    }

                    content(object : ArchiveSharingPageScope {
                        override val animatedContentScope: AnimatedContentScope
                            get() = this@composable
                        override val navController: NavHostController
                            get() = navController

                        override fun dismiss() {
                            onDismissRequested()
                        }

                        private fun updateDecorations(
                            buildIdConstraint: Int? = null,
                            transform: (PageNavDecorations) -> PageNavDecorations,
                        ): Int? {
                            return if (pageNavStack.size <= positionInStack) {
                                val entry = PageNavStackEntry(
                                    routeName,
                                    transform(PageNavDecorations()),
                                    ownerId = ++latestBuildId
                                )
                                // build id constraint not applied here
                                pageNavStack.add(entry)
                                entry.ownerId
                            } else {
                                val entry = pageNavStack[positionInStack]
                                if (buildIdConstraint?.let { it == entry.ownerId } == false) {
                                    return null
                                }
                                val buildId = ++latestBuildId
                                pageNavStack[positionInStack] =
                                    entry.copy(
                                        decorations = transform(entry.decorations),
                                        ownerId = buildId
                                    )
                                buildId
                            }
                        }

                        @Composable
                        override fun ActionButtons(
                            content: @Composable (RowScope.() -> Unit),
                        ) {
                            DisposableEffect(content) {
                                val buildId = updateDecorations {
                                    it.copy(actionButton = content)
                                }
                                onDispose {
                                    updateDecorations(buildId) {
                                        it.copy(actionButton = null)
                                    }
                                }
                            }
                        }

                        @Composable
                        override fun TopBarTitle(content: @Composable (() -> Unit)) {
                            DisposableEffect(content) {
                                val buildId = updateDecorations {
                                    it.copy(topBarTitle = content)
                                }
                                onDispose {
                                    updateDecorations(buildId) {
                                        it.copy(topBarTitle = null)
                                    }
                                }
                            }
                        }
                    }, entry)
                }
            }

            starterOptions = buildList {
                builder(object : ArchiveSharingDialogBuilder {
                    override fun starterOption(
                        routeName: String,
                        icon: @Composable (() -> Unit),
                        label: @Composable (() -> Unit),
                        content: @Composable (ArchiveSharingPageScope.(NavBackStackEntry) -> Unit),
                    ) {
                        composable(routeName, content)
                        add(StarterPageOption(routeName, icon, label))
                    }

                    override fun nestedPage(
                        routeName: String,
                        content: @Composable (ArchiveSharingPageScope.(NavBackStackEntry) -> Unit),
                    ) {
                        composable(routeName, content)
                    }
                })
            }
            composable("starter") {
                StarterPage(navController, starterOptions)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall),
            modifier = Modifier.padding(
                start = PaddingNormal,
                end = PaddingNormal,
                bottom = PaddingNormal,
                top = PaddingSmall
            )
        ) {
            OutlinedButton(onClick = {
                model.cancel()
                onDismissRequested()
            }) {
                Text(stringResource(Res.string.cancel_para))
            }
            Spacer(Modifier.weight(1f))
            pageNavStack.lastOrNull()?.decorations?.actionButton?.invoke(this)
        }
    }
}

@Composable
private fun StarterPage(navController: NavHostController, options: List<StarterPageOption>) {
    Column(Modifier.padding(horizontal = PaddingNormal)) {
        options.forEach { option ->
            Surface(
                onClick = {
                    navController.navigate(option.routeName)
                },
                shape = RoundedCornerShape(size = 12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
                    modifier = Modifier.padding(PaddingNormal)
                ) {
                    option.icon()
                    option.label()
                }
            }
        }
    }
}

data class StarterPageOption(
    val routeName: String,
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
)

interface ArchiveSharingDialogBuilder {
    fun starterOption(
        routeName: String,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
        content: @Composable (ArchiveSharingPageScope.(NavBackStackEntry) -> Unit),
    )

    fun nestedPage(
        routeName: String,
        content: @Composable ArchiveSharingPageScope.(NavBackStackEntry) -> Unit,
    )
}

interface ArchiveSharingPageScope {
    val animatedContentScope: AnimatedContentScope
    val navController: NavHostController
    fun dismiss()

    @Composable
    fun ActionButtons(content: @Composable RowScope.() -> Unit)

    @Composable
    fun TopBarTitle(content: @Composable () -> Unit)
}

private data class PageNavDecorations(
    val actionButton: (@Composable RowScope.() -> Unit)? = null,
    val topBarTitle: (@Composable () -> Unit)? = null,
)

private data class PageNavStackEntry(
    val routeName: String,
    val decorations: PageNavDecorations = PageNavDecorations(),
    val ownerId: Int,
)

fun ArchiveSharingDialogBuilder.exportToFileOption(model: ArchiveSharingViewModel) {
    starterOption(
        routeName = "export_to_file",
        icon = {
            Icon(
                painterResource(Res.drawable.baseline_archive_arrow_down_outline),
                contentDescription = null
            )
        },
        label = {
            Text(stringResource(Res.string.export_to_file_para))
        },
    ) {
        var cancelled by remember { mutableStateOf(false) }
        var trials by remember { mutableIntStateOf(0) }
        LaunchedEffect(trials) {
            val archiveFile = FileKit.openFileSaver(
                suggestedName = model.describeSelection(),
                extension = "psarchive"
            )
            cancelled = archiveFile == null
            if (archiveFile != null) {
                model.exportToFile.send(archiveFile)
                dismiss()
            }
        }

        Column(Modifier.padding(horizontal = PaddingBig)) {
            if (!cancelled) {
                Text(stringResource(Res.string.continue_in_system_file_manager_para))
            } else {
                Text(stringResource(Res.string.action_was_cancelled_para))
                ActionButtons {
                    Button(onClick = {
                        trials++
                    }) {
                        Text(stringResource(Res.string.retry_para))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ArchiveSharingDialogBuilder.uploadToCommunity(model: ArchiveSharingViewModel) {
    starterOption(
        routeName = "upload_to_community",
        icon = {
            Icon(painterResource(Res.drawable.outline_cloud_upload), contentDescription = null)
        },
        label = { Text(stringResource(Res.string.upload_to_community_para)) }
    ) {
        LaunchedEffect(true) {
            model.uploadToCommunity.send(Unit)
        }
        val pageCoroutine = rememberCoroutineScope()
        val serverInfo by model.serverInfo.collectAsState()

        AnimatedContent(
            model.uploadState,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { uploadState ->
            Column(
                Modifier.padding(horizontal = PaddingBig).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uploadState) {
                    is UploadArchive.ArchiveNameRequired -> {
                        var buffer by rememberSaveable(stateSaver = protobufSaver()) {
                            mutableStateOf(LengthConstrainedTextFieldBuffer(""))
                        }
                        LaunchedEffect(true) {
                            val contentDescription = model.describeSelection()
                            if (buffer.value.isEmpty()) {
                                buffer = LengthConstrainedTextFieldBuffer.of(
                                    contentDescription,
                                    serverInfo?.maxNameLength?.value
                                )
                            }
                        }

                        LengthConstrainedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            state = buffer,
                            onStateChange = { buffer = it },
                            label = { Text(stringResource(Res.string.name_of_entry_para)) },
                            maxLength = serverInfo?.maxNameLength?.value
                        )
                        ActionButtons {
                            Button(
                                onClick = {
                                    pageCoroutine.launch {
                                        uploadState.submission.send(buffer.value)
                                    }
                                },
                                enabled = !buffer.isOversized
                            ) {
                                Text(stringResource(Res.string.upload_para))
                            }
                        }
                    }

                    is UploadArchive.RegistrationRequired -> {
                        TopBarTitle {
                            Text(stringResource(Res.string.registration_required_para))
                        }

                        var ownerNameBuffer by rememberSaveable(stateSaver = protobufSaver()) {
                            mutableStateOf(LengthConstrainedTextFieldBuffer(AppSettings.clientName.value))
                        }
                        LengthConstrainedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            state = ownerNameBuffer,
                            onStateChange = { ownerNameBuffer = it },
                            label = { Text(stringResource(Res.string.owner_name_para)) },
                            maxLength = serverInfo?.maxNameLength?.value
                        )
                        Spacer(Modifier.height(PaddingSmall))

                        var deviceNameBuffer by rememberSaveable(stateSaver = protobufSaver()) {
                            mutableStateOf(LengthConstrainedTextFieldBuffer(ownerNameBuffer.value))
                        }
                        LengthConstrainedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            state = deviceNameBuffer,
                            onStateChange = { deviceNameBuffer = it },
                            label = { Text(stringResource(Res.string.device_name_para)) },
                            maxLength = serverInfo?.maxNameLength?.value
                        )

                        ActionButtons {
                            Button(
                                onClick = {
                                    pageCoroutine.launch {
                                        uploadState.submission.send(
                                            CommunityRegistrationInfo(
                                                clientName = deviceNameBuffer.value,
                                                ownerName = ownerNameBuffer.value
                                            )
                                        )
                                    }
                                },
                                enabled = !ownerNameBuffer.isOversized && !deviceNameBuffer.isOversized
                            ) {
                                Text(stringResource(Res.string.upload_para))
                            }
                        }
                    }

                    is UploadArchive.Failure -> {
                        Icon(
                            painterResource(Res.drawable.outline_alert_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(buildString {
                            appendLine(stringResource(Res.string.http_error_para))
                            append(uploadState.message)
                        })
                    }

                    is UploadArchive.InProgress -> {
                        val stats by uploadState.stats.collectAsState()
                        val progress =
                            stats.bytesTotal?.let { total -> stats.bytesSent.toFloat() / total }
                        if (progress == null) {
                            CircularWavyProgressIndicator()
                        } else {
                            CircularWavyProgressIndicator(progress = { progress })
                        }
                    }

                    is UploadArchive.Success -> {
                        Icon(
                            painterResource(Res.drawable.baseline_check_circle_outline),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            stringResource(Res.string.shared_with_the_community_para),
                            textAlign = TextAlign.Center
                        )
                        ActionButtons {
                            val navController = currentNavController()
                            val coroutine = rememberCoroutineScope()
                            var isFetchingMetadata by remember { mutableStateOf(false) }
                            Button(onClick = {
                                coroutine.launch {
                                    isFetchingMetadata = true
                                    val metadata = runCatching {
                                        model.communityService
                                            .first()
                                            .getArchiveMetadata(uploadState.archiveId)
                                    }
                                    metadata.getOrNull()?.let {
                                        navController.navigate(ArchivePreviewRouteParams(metadata = it))
                                        isFetchingMetadata = false
                                        dismiss()
                                    }
                                }
                            }) {
                                if (isFetchingMetadata) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(PaddingSmall))
                                }
                                Text(stringResource(Res.string.reveal_para))
                            }
                        }
                    }

                    null -> {
                        CircularWavyProgressIndicator()
                        Text(
                            stringResource(Res.string.waiting_for_service_para),
                            textAlign = TextAlign.Center
                        )
                    }

                    is UploadArchive.SignOffRequired -> {
                        Icon(
                            painterResource(Res.drawable.outline_alert_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(stringResource(Res.string.must_reregister_to_proceed_para))
                        uploadState.serverMessage?.let {
                            Text(stringResource(Res.string.from_server_x_para, it))
                        }
                        ActionButtons {
                            Button(
                                onClick = {
                                    pageCoroutine.launch { uploadState.proceed.send(Unit) }
                                },
                                colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(stringResource(Res.string.clear_token_para))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
@Serializable
private data class LengthConstrainedTextFieldBuffer(
    val value: String,
    val isOversized: Boolean = false,
) {
    companion object {
        fun of(value: String, maxLength: Int? = null): LengthConstrainedTextFieldBuffer {
            return LengthConstrainedTextFieldBuffer(
                value = value,
                isOversized = maxLength?.let { maxLength -> maxLength < value.length } == true
            )
        }
    }
}

@Composable
private fun LengthConstrainedTextField(
    modifier: Modifier = Modifier,
    state: LengthConstrainedTextFieldBuffer,
    onStateChange: (LengthConstrainedTextFieldBuffer) -> Unit,
    label: (@Composable () -> Unit)? = null,
    maxLength: Int? = null,
) {
    TextField(
        modifier = modifier,
        value = state.value,
        onValueChange = {
            onStateChange(LengthConstrainedTextFieldBuffer.of(it, maxLength))
        },
        label = label,
        supportingText = {
            if (maxLength != null) {
                Text("${state.value.length} / $maxLength")
            }
        },
        isError = state.isOversized
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isNavigateForward(controller: NavHostController): Boolean {
    if (initialState.destination.route == "starter") {
        return true
    }
    if (targetState.destination.route == "starter") {
        return false
    }
    // fixme: this following method is buggy, preventing 3 or more levels of nesting to be animated correctly
    val initialBackStackPos =
        controller.currentBackStack.value.indexOfFirst { it.destination == initialState.destination }
    val targetBackStackPos =
        controller.currentBackStack.value.indexOfFirst { it.destination == targetState.destination }
    return targetBackStackPos == -1 || initialBackStackPos < targetBackStackPos
}
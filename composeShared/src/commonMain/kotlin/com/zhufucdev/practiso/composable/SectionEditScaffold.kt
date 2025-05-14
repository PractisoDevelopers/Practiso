package com.zhufucdev.practiso.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.zhufucdev.practiso.composition.currentNavController
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.viewmodel.SectionEditViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.baseline_select_all
import resources.n_items_selected_para
import resources.navigate_up_para
import resources.select_all_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : PractisoOption, M : SectionEditViewModel<T>> SectionEditScaffold(
    items: List<T>,
    initialTopItemIndex: Int,
    model: M,
    bottomBar: @Composable () -> Unit,
) {
    val coroutine = rememberCoroutineScope()
    val navController = currentNavController()
    val topBarBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        pluralStringResource(
                            Res.plurals.n_items_selected_para,
                            model.selection.size,
                            model.selection.size
                        )
                    )
                },
                navigationIcon = {
                    PlainTooltipBox(stringResource(Res.string.navigate_up_para)) {
                        IconButton(
                            onClick = {
                                navController.navigateUp()
                            },
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(Res.string.navigate_up_para)
                            )
                        }
                    }
                },
                scrollBehavior = topBarBehavior
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialTopItemIndex)
        LazyColumn(
            Modifier.padding(padding).nestedScroll(topBarBehavior.nestedScrollConnection),
            state = listState
        ) {
            items(items.size, key = { items[it].id }) { idx ->
                val option = items[idx]
                val checked = option.id in model.selection

                Box(Modifier.clickable {
                    coroutine.launch {
                        if (checked) {
                            model.commonEvents.deselect.send(option.id)
                        } else {
                            model.commonEvents.select.send(option.id)
                        }
                    }
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(PaddingNormal)
                    ) {
                        PractisoOptionView(option, modifier = Modifier.weight(1f))
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }

                if (idx < items.size - 1) {
                    Box(Modifier.padding(start = PaddingNormal)) {
                        HorizontalSeparator()
                    }
                }
            }
        }
    }
}

@Composable
fun <T : PractisoOption> RowScope.CommonActions(
    model: SectionEditViewModel<T>,
    items: List<T>,
) {
    val coroutine = rememberCoroutineScope()
    PlainTooltipBox(stringResource(Res.string.select_all_para)) {
        IconButton(
            onClick = {
                coroutine.launch {
                    if (model.selection.size == items.size) {
                        model.commonEvents.clearSelection.send(Unit)
                    } else {
                        model.commonEvents.selectAll.send(items.map { it.id })
                    }
                }
            }
        ) {
            Icon(
                painterResource(Res.drawable.baseline_select_all),
                contentDescription = null
            )
        }
    }
}

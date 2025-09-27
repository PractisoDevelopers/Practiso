package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingSmall
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import resources.Res
import resources.collapse_filter_para

@OptIn(ExperimentalSharedTransitionApi::class)
fun <T, K> LazyListScope.filter(
    modifier: Modifier = Modifier,
    controller: FilterController<T, K>,
    targetHeightExpanded: Dp = 200.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    sort: (Pair<FilterGroup<K>, List<T>>, Pair<FilterGroup<K>, List<T>>) -> Int = { a, b -> a.second.size - b.second.size },
    key: ((K) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(FilterGroup<K>) -> Unit,
) {
    controller.impl.targetHeightDp = targetHeightExpanded.value
    fun getKey(group: FilterGroup<K>, index: Int): Any {
        if (key == null) {
            return index
        }
        return when (group) {
            is SomeGroup<K> -> key(group.value)
            is NoGroup -> "no_group"
        }
    }

    stickyHeader {
        val rootCoroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current
        val lazyRowState = rememberLazyListState()
        val sortedGroups: List<FilterGroup<K>> = remember(sort, controller) {
            controller.groupedItems.entries
                .sortedWith { a, b -> sort(a.toPair(), b.toPair()) }
                .map(Map.Entry<FilterGroup<K>, List<T>>::key)
        }

        SharedTransitionLayout(
            modifier then Modifier.pointerInput(
                controller,
                lazyRowState.canScrollForward,
                lazyRowState.canScrollBackward
            ) {
                if (!lazyRowState.canScrollForward && !lazyRowState.canScrollBackward) {
                    return@pointerInput
                }

                var originalExpanded = controller.expanded
                detectVerticalDragGestures(
                    onDragStart = {
                        originalExpanded = controller.expanded
                    },
                    onVerticalDrag = { change, dragAmount ->
                        rootCoroutineScope.launch {
                            controller.impl.snapAnimatorTo(controller.impl.expandedColumnHeightAnimator.value + dragAmount.toDp().value)
                        }
                    },
                    onDragCancel = {
                        rootCoroutineScope.launch {
                            controller.animateExpansion(originalExpanded)
                        }
                    },
                    onDragEnd = {
                        rootCoroutineScope.launch {
                            controller.animateExpansion(controller.impl.expandedColumnHeightAnimator.value > controller.impl.initialHeightDp)
                        }
                    }
                )
            }
        ) {
            AnimatedContent(controller.impl.expansionVisible) { expansionVisible ->
                if (expansionVisible) {
                    val expansionColumnHeightDp by controller.impl.expandedColumnHeightAnimator.asState()
                    Column(
                        Modifier.fillParentMaxWidth()
                            .padding(contentPadding)
                            .heightIn(max = expansionColumnHeightDp.dp + 48.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = horizontalArrangement,
                            verticalArrangement = verticalArrangement,
                            modifier = Modifier
                                .clipToBounds()
                                .height(expansionColumnHeightDp.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            sortedGroups.forEachIndexed { index, group ->
                                Box(
                                    if (lazyRowState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { index < it } == true) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(getKey(group, index)),
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    } else {
                                        Modifier
                                    }
                                ) {
                                    itemContent(group)
                                }
                            }
                        }
                        if (controller.impl.collapseButtonVisible) {
                            TextButton(
                                modifier = Modifier.fillParentMaxWidth().heightIn(min = 48.dp),
                                onClick = {
                                    rootCoroutineScope.launch {
                                        controller.animateExpansion(false)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(Res.string.collapse_filter_para)
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        Modifier
                            .fillParentMaxWidth()
                            .onGloballyPositioned {
                                controller.impl.initialHeightDp = with(density) {
                                    it.size.height.toDp().value
                                }
                            },
                        state = lazyRowState,
                        reverseLayout = reverseLayout,
                        horizontalArrangement = horizontalArrangement,
                        verticalAlignment = verticalAlignment,
                        contentPadding = contentPadding,
                    ) {
                        items(
                            count = sortedGroups.size,
                            key = { getKey(sortedGroups[it], it) }) { index ->
                            Box(
                                Modifier.sharedElement(
                                    rememberSharedContentState(getKey(sortedGroups[index], index)),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                            ) {
                                itemContent(sortedGroups[index])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T, K> LazyListScope.filteredItems(
    controller: FilterController<T, K>,
    key: ((List<T>, Int) -> Any)? = null,
    sort: ((T, T) -> Int)? = null,
    itemContent: @Composable LazyItemScope.(List<T>, Int) -> Unit,
) {
    val items by derivedStateOf {
        controller.impl.items.let {
            if (sort != null) it.sortedWith(sort)
            else it
        }.filter { item ->
            val itemGroups = controller.impl.groupSelector(item)
            if (itemGroups.isEmpty() && controller.selectedGroups.contains(NoGroup as FilterGroup<K>)) {
                true
            } else {
                itemGroups.any { SomeGroup(it) in controller.selectedGroups }
            }
        }
    }
    items(count = items.size, key = key?.let { key -> { key(items, it) } }) { index ->
        itemContent(items, index)
    }
}

fun <T, K> LazyListScope.filteredItems(
    controller: FilterController<T, K>,
    key: ((T) -> Any)? = null,
    sort: ((T, T) -> Int)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    filteredItems(
        controller,
        key = key?.let { key -> { items, index -> key(items[index]) } },
        sort = sort
    ) { items, index ->
        itemContent(items[index])
    }
}

@Stable
interface FilterController<T, K> {
    val groupedItems: Map<FilterGroup<K>, List<T>>
    val selectedGroups: Set<FilterGroup<K>>
    val expanded: Boolean
    suspend fun animateExpansion(expanded: Boolean)
    fun toggleGroup(group: FilterGroup<K>, selected: Boolean? = null)
}

@Composable
fun <T, K> rememberFilterController(
    items: Collection<T>,
    groupSelector: (T) -> Collection<K>,
): FilterController<T, K> =
    remember(items, groupSelector) { FilterControllerImpl(items, groupSelector) }

fun <T, K> FilterController(
    items: Collection<T>,
    groupSelector: (T) -> Collection<K>,
): FilterController<T, K> =
    FilterControllerImpl(items, groupSelector)

sealed interface FilterGroup<K>
data object NoGroup : FilterGroup<Any>

@JvmInline
value class SomeGroup<K>(val value: K) : FilterGroup<K>


@Suppress("UNCHECKED_CAST")
@Stable
private class FilterControllerImpl<T, K>(
    val items: Collection<T>,
    val groupSelector: (T) -> Collection<K>,
) :
    FilterController<T, K> {
    override val groupedItems by derivedStateOf {
        buildMap {
            items.forEach { item ->
                val groups =
                    groupSelector(item).map { SomeGroup(it) }.takeIf { it.isNotEmpty() } ?: listOf(
                        NoGroup as FilterGroup<K>
                    )
                groups.forEach { group ->
                    if (!containsKey(group)) {
                        put(group, mutableListOf(item))
                    } else {
                        get(group)!!.add(item)
                    }
                }
            }
        }
    }
    override val selectedGroups: MutableSet<FilterGroup<K>> = mutableStateSetOf()
    override var expanded by mutableStateOf(false)
        private set

    var expansionVisible by mutableStateOf(false)
        private set
    var collapseButtonVisible by mutableStateOf(false)
        private set
    var targetHeightDp: Float = 200f
    var initialHeightDp: Float = 0f
    val expandedColumnHeightAnimator = Animatable(initialValue = initialHeightDp)

    override suspend fun animateExpansion(expanded: Boolean) {
        if (expanded) {
            expansionVisible = true
            collapseButtonVisible = true
            expandedColumnHeightAnimator.animateTo(targetHeightDp)
        } else {
            collapseButtonVisible = false
            expandedColumnHeightAnimator.animateTo(initialHeightDp)
            expansionVisible = false
        }
        this.expanded = expanded
    }

    override fun toggleGroup(group: FilterGroup<K>, selected: Boolean?) {
        val selected = selected ?: (group !in selectedGroups)
        if (selected) {
            selectedGroups.add(group)
        } else {
            selectedGroups.remove(group)
        }
    }

    suspend fun snapAnimatorTo(heightDp: Float) {
        expansionVisible = heightDp > initialHeightDp
        expanded = expansionVisible
        expandedColumnHeightAnimator.snapTo(heightDp)
    }
}

private val <T, K> FilterController<T, K>.impl get() = this as FilterControllerImpl<T, K>

@Preview
@Composable
fun FilterPreview() {
    val controller = rememberFilterController(
        items = listOf(
            "Carrot" to listOf("Fruit", "Vegetable"),
            "Apple" to listOf("Fruit"),
            "How to become Trump" to listOf("Book"),
            "Homo Sapiens: A Brief History of Mankind (or whatever)" to listOf("Book"),
            "uPhone 69 Pro Max" to listOf("Electronic"),
            "The Washer: Laundry Hunter" to listOf("Game"),
            "Game Making Toolkit" to listOf("Misc"),
            "Surreal Engine 5 Commerical License" to listOf("Software"),
            "Crackers" to listOf("Snack"),
            "Look&Look Water Bottle" to listOf("Life"),
            "Samsnug Smart Fridge" to listOf("Home"),
            "Shitcoin" to listOf("Web3"),
        ),
        groupSelector = Pair<String, List<String>>::second
    )
    val coroutine = rememberCoroutineScope()
    LazyColumn {
        filter(
            Modifier.fillMaxWidth(),
            itemContent = {
                ChipSkeleton(
                    modifier = Modifier.clickable(onClick = { controller.toggleGroup(it) }),
                    selected = it in controller.selectedGroups,
                    label = {
                        if (it is SomeGroup) Text(it.value)
                        else Text("Stranded")
                    },
                )
            },
            controller = controller,
            key = { it },
            verticalArrangement = Arrangement.spacedBy(PaddingSmall),
            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
        )
        filteredItems(
            controller,
            key = { it.first }
        ) {
            Text(it.first)
        }
        item {
            Button(
                onClick = {
                    coroutine.launch {
                        controller.animateExpansion(!controller.expanded)
                    }
                }
            ) {
                Text("Toggle")
            }
        }
    }
}

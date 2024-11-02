package com.zhufucdev.practiso.composable

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.style.PaddingNormal

@Composable
fun FloatingPopupButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    autoCollapse: Boolean = false,
    content: FloatingPopupItemBuildScope.() -> Unit,
) {
    var showActions by remember { mutableStateOf(false) }
    var actionTransition by remember {
        mutableFloatStateOf(0f)
    }
    val rotation by remember {
        derivedStateOf {
            actionTransition * 135f
        }
    }
    val translation by remember {
        derivedStateOf {
            (1 - actionTransition) * 24f
        }
    }
    LaunchedEffect(expanded) {
        if (expanded) {
            showActions = true
        }
        animate(
            initialValue = actionTransition,
            targetValue = if (expanded) 1f else 0f
        ) { v, _ ->
            actionTransition = v
        }
        if (!expanded) {
            showActions = false
        }
    }

    val items = remember(content) {
        buildList<@Composable (Modifier) -> Unit> {
            val scope = object : FloatingPopupItemBuildScope {
                override fun item(
                    label: @Composable () -> Unit,
                    icon: @Composable () -> Unit,
                    onClick: () -> Unit,
                ) {
                    add {
                        FloatingPopupItem(label, icon, onClick = {
                            if (autoCollapse) {
                                onExpandedChange(false)
                            }
                            onClick()
                        }, it)
                    }
                }
            }
            content(scope)
        }
    }
    Column(horizontalAlignment = Alignment.End) {
        if (showActions) {
            items.forEachIndexed { index, it ->
                it(
                    Modifier
                        .offset(y = ((items.size - index) * translation).dp)
                        .alpha(actionTransition)
                )
                Spacer(Modifier.height(PaddingNormal))
            }
        }

        FloatingActionButton(
            content = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "",
                    modifier = Modifier.rotate(rotation)
                )
            },
            onClick = { onExpandedChange(!expanded) },
        )
    }
}

interface FloatingPopupItemBuildScope {
    fun item(
        label: @Composable () -> Unit,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
    )
}

@Composable
private fun FloatingPopupItem(
    label: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleSmall
        ) {
            label()
        }
        Spacer(Modifier.width(PaddingNormal))
        FloatingActionButton(
            onClick = onClick,
            content = icon,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            modifier = modifier
        )
    }
}
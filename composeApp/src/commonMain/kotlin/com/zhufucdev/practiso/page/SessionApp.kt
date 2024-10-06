package com.zhufucdev.practiso.page

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.FabClaimScope
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.globalViewModel
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.SessionViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_check_circle_outline
import practiso.composeapp.generated.resources.baseline_timelapse
import practiso.composeapp.generated.resources.create_para
import practiso.composeapp.generated.resources.done_questions_completed_in_total
import practiso.composeapp.generated.resources.get_started_by_para
import practiso.composeapp.generated.resources.recently_used_para
import practiso.composeapp.generated.resources.take_completeness
import practiso.composeapp.generated.resources.welcome_to_app_para
import kotlin.math.min

@Composable
fun FabClaimScope.SessionApp(sessionViewModel: SessionViewModel = globalViewModel()) {
    val takeStats by sessionViewModel.recentTakeStats.collectAsState(null)

    floatingActionButton {
        ExtendedFloatingActionButton(
            onClick = {},
            icon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            },
            text = {
                Text(stringResource(Res.string.create_para))
            }
        )
    }

    if (takeStats?.isEmpty() == true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("👋", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(PaddingNormal))
            Text(
                stringResource(Res.string.welcome_to_app_para),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(Res.string.get_started_by_para),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(
            Modifier.verticalScroll(rememberScrollState())
                .padding(top = PaddingNormal)
        ) {
            SectionCaption(
                stringResource(Res.string.recently_used_para),
                Modifier.padding(start = PaddingNormal)
            )
            Spacer(Modifier.height(PaddingSmall))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PaddingNormal)) {
                item("start_spacer") {
                    Spacer(Modifier)
                }
                takeStats?.let {
                    items(it, TakeStat::id) { session ->
                        Card { TakeContent(session) }
                    }
                } ?: items(3) {
                    Card { TakeSkeleton() }
                }
                item("end_spacer") {
                    Spacer(Modifier)
                }
            }
        }
    }
}

@Composable
fun TakeContent(model: TakeStat) {
    TakeSkeleton(
        icon = {
            Icon(
                painter = painterResource(
                    if (model.countQuizDone >= model.countQuizTotal) {
                        Res.drawable.baseline_check_circle_outline
                    } else {
                        Res.drawable.baseline_timelapse
                    }
                ),
                contentDescription = stringResource(Res.string.take_completeness)
            )
        },
        label = {
            Text(
                text = model.name,
                overflow = TextOverflow.Ellipsis
            )
        },
        content = {
            Text(
                pluralStringResource(
                    Res.plurals.done_questions_completed_in_total,
                    min(model.countQuizDone, 10).toInt(),
                    model.countQuizDone,
                    model.countQuizTotal
                )
            )
        },
        progress = model.countQuizDone.toFloat() / model.countQuizTotal
    )
}

@Composable
fun TakeSkeleton(
    icon: @Composable () -> Unit = {
        Box(Modifier.size(32.dp).shimmerBackground(CircleShape))
    },
    label: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxWidth()
                .height(LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground()
        )
    },
    content: @Composable () -> Unit = label,
    progress: Float = 0f,
) {
    val p by animateFloatAsState(targetValue = progress)
    Box {
        Box(Modifier.matchParentSize()) {
            Spacer(
                Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    .fillMaxHeight()
                    .fillMaxWidth(p)
            )
        }
        Column(Modifier.padding(PaddingNormal).width(200.dp)) {
            icon()
            Spacer(Modifier.height(PaddingSmall))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleSmall
            ) {
                label()
            }
            Spacer(Modifier.height(PaddingSmall))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelMedium
            ) {
                content()
            }
        }
    }
}


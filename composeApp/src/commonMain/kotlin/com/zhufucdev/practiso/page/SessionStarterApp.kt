package com.zhufucdev.practiso.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhufucdev.practiso.composable.shimmerBackground
import com.zhufucdev.practiso.composition.composeFromBottomUp
import com.zhufucdev.practiso.composition.globalViewModel
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import com.zhufucdev.practiso.viewmodel.SessionSelectorViewModel
import com.zhufucdev.practiso.viewmodel.SessionStarterAppViewModel
import kotlinx.serialization.Serializable

@Composable
fun SessionStarter(
    initModel: SessionStarterInitialDataModel = SessionStarterInitialDataModel(),
    appModel: SessionStarterAppViewModel = globalViewModel(),
    selectorModel: SessionSelectorViewModel = viewModel(factory = SessionSelectorViewModel.Factory),
) {
    composeFromBottomUp("fab", null)

    val items by appModel.items.collectAsState(null)
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(PaddingNormal))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PaddingNormal)) {
            item("start_spacer") {
                Spacer(Modifier.width(PaddingSmall))
            }
            items(4) {
                DimensionSkeleton()
            }
            item("end_spacer") {
                Spacer(Modifier.width(PaddingSmall))
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingNormal)) {
            items(8) { index ->
                QuizSkeleton(modifier = Modifier.padding(horizontal = PaddingNormal))
                if (index < 7) {
                    Spacer(Modifier.height(PaddingNormal))
                    Spacer(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.size(100.dp, LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground()
        )
    },
    tailingIcon: @Composable () -> Unit = {},
) {
    OutlinedCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingNormal),
            modifier = Modifier.padding(PaddingSmall)
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.labelLarge
            ) {
                label()
            }
            tailingIcon()
        }
    }
}

@Composable
private fun QuizSkeleton(
    label: @Composable () -> Unit = {
        Spacer(
            Modifier.fillMaxWidth().height(LocalTextStyle.current.lineHeight.value.dp)
                .shimmerBackground(RoundedCornerShape(PaddingSmall))
        )
    },
    preview: @Composable () -> Unit = {
        repeat(2) {
            Spacer(
                Modifier.fillMaxWidth(0.6f).height(LocalTextStyle.current.lineHeight.value.dp)
                    .shimmerBackground(RoundedCornerShape(PaddingSmall))
            )
        }
    },
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaddingSmall), modifier = modifier) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge
        ) {
            label()
        }
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium
        ) {
            preview()
        }
    }
}

@Serializable
data class SessionStarterInitialDataModel(
    val quizIds: List<Long> = emptyList(),
    val dimensionIds: List<Long> = emptyList(),
)

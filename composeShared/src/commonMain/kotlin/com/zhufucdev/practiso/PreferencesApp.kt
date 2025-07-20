package com.zhufucdev.practiso

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composable.PlainTooltipBox
import com.zhufucdev.practiso.composable.PractisoOptionSkeleton
import com.zhufucdev.practiso.composable.SectionCaption
import com.zhufucdev.practiso.composable.SharedElementTransitionKey
import com.zhufucdev.practiso.composable.SharedElementTransitionPopup
import com.zhufucdev.practiso.composable.modelFeatureString
import com.zhufucdev.practiso.composition.BottomUpComposableScope
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.style.PaddingBig
import com.zhufucdev.practiso.style.PaddingNormal
import com.zhufucdev.practiso.style.PaddingSmall
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.answering_para
import resources.compatibility_mode_para
import resources.frame_embedding_model_para
import resources.known_model_names_title
import resources.navigate_up_para
import resources.reveals_accuracy_gets_feedback_for_wrong_answers_para
import resources.settings_para
import resources.show_accuracy_para
import resources.smart_recommendations_para
import resources.use_only_cpu_for_inference_para

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesApp(model: SettingsModel = AppSettings) {
    val coroutine = rememberCoroutineScope()

    BottomUpComposableScope { buc ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_para)) },
                    navigationIcon = {
                        PlainTooltipBox(stringResource(Res.string.navigate_up_para)) {
                            IconButton(onClick = {
                                coroutine.launch {
                                    Navigator.navigate(Navigation.Backward)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(Modifier.padding(paddingValues)) {
                item {
                    SectionCaption(
                        stringResource(Res.string.answering_para),
                        Modifier.padding(horizontal = PaddingBig)
                    )
                }
                item {
                    val value: Boolean by model.showAccuracy.collectAsState()
                    Preference(
                        label = stringResource(Res.string.show_accuracy_para),
                        preview = stringResource(Res.string.reveals_accuracy_gets_feedback_for_wrong_answers_para),
                        tailing = {
                            Switch(
                                checked = value,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            model.showAccuracy.tryEmit(!value)
                        }
                    )
                }

                item {
                    SectionCaption(
                        stringResource(Res.string.smart_recommendations_para),
                        Modifier.padding(horizontal = PaddingBig).padding(top = PaddingBig)
                    )
                }

                item {
                    val value by model.feiModelIndex.collectAsState()
                    SharedElementTransitionPopup(
                        key = "fei_model_preference",
                        popup = {
                            val currentModelIndex by model.feiModelIndex.collectAsState()
                            Card {
                                Column(Modifier.padding(vertical = PaddingBig)) {
                                    Text(
                                        stringResource(Res.string.frame_embedding_model_para),
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(bottom = PaddingNormal)
                                            .padding(horizontal = PaddingBig)
                                    )
                                    KnownModels.forEachIndexed { index, mlModel ->
                                        MlModelSelection(
                                            modifier = Modifier.padding(
                                                vertical = PaddingNormal,
                                                horizontal = PaddingBig
                                            ),
                                            featureContainerModifier = Modifier.padding(
                                                horizontal = PaddingBig
                                            ),
                                            model = mlModel,
                                            index = index,
                                            selected = currentModelIndex == index,
                                            onSelectedChange = {
                                                model.feiModelIndex.tryEmit(index)
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        sharedElement = {
                            PreferenceSkeleton(
                                label = stringResource(Res.string.frame_embedding_model_para),
                                preview = stringArrayResource(Res.array.known_model_names_title)[value],
                                modifier = it
                            )
                        },
                        content = {
                            Preference(
                                label = stringResource(Res.string.frame_embedding_model_para),
                                preview = stringArrayResource(Res.array.known_model_names_title)[value],
                                onClick = {
                                    coroutine.launch {
                                        expand()
                                    }
                                },
                                modifier = Modifier.sharedElement()
                            )
                        }
                    )
                }

                item {
                    val value by model.feiCompatibilityMode.collectAsState()
                    Preference(
                        label = stringResource(Res.string.compatibility_mode_para),
                        preview = stringResource(Res.string.use_only_cpu_for_inference_para),
                        tailing = {
                            Switch(
                                checked = value,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            model.feiCompatibilityMode.tryEmit(!value)
                        }
                    )
                }
            }
        }

        buc.compose(SharedElementTransitionKey)
    }
}

@Composable
private fun Preference(
    modifier: Modifier = Modifier,
    label: String,
    preview: String? = null,
    tailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    PreferenceSkeleton(
        modifier = Modifier.clickable(onClick = onClick) then modifier,
        label = label,
        preview = preview,
        tailing = tailing
    )
}

@Composable
fun PreferenceSkeleton(
    modifier: Modifier = Modifier,
    label: String,
    preview: String? = null,
    tailing: @Composable () -> Unit = {},
) {
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(
                horizontal = PaddingBig,
                vertical = PaddingNormal
            )
        ) {
            PractisoOptionSkeleton(
                modifier = Modifier.weight(1f),
                label = { Text(label) },
                preview = {
                    if (preview != null) {
                        Text(preview)
                    }
                })
            tailing()
        }
    }
}

@Composable
private fun MlModelSelection(
    modifier: Modifier = Modifier,
    featureContainerModifier: Modifier = Modifier,
    model: MlModel,
    index: Int = KnownModels.indexOf(model),
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    Column {
        Box(Modifier.clickable {
            onSelectedChange(!selected)
        }) {
            Row(modifier) {
                Text(
                    stringArrayResource(Res.array.known_model_names_title)[index],
                    modifier = Modifier.weight(1f)
                )

                RadioButton(
                    selected = selected,
                    onClick = null
                )
            }
        }
        AnimatedVisibility(visible = selected) {
            @Suppress("SimplifiableCallChain") // this cannot be simplified because joinToString is not Composable
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = featureContainerModifier
            ) {
                model.features.map { modelFeatureString(it) }.sortedBy { it.length }
                    .forEach { featureText ->
                        Box(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(PaddingSmall)
                            )
                        ) {
                            Text(
                                featureText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(PaddingSmall)
                            )
                        }
                    }
            }
        }
    }
}
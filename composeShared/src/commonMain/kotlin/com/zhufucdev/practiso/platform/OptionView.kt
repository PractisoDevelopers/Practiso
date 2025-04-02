package com.zhufucdev.practiso.platform

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.datamodel.DimensionOption
import com.zhufucdev.practiso.datamodel.PractisoOption
import com.zhufucdev.practiso.datamodel.QuizOption
import com.zhufucdev.practiso.datamodel.SessionCreator
import com.zhufucdev.practiso.datamodel.SessionOption
import com.zhufucdev.practiso.datamodel.TemplateOption
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.empty_span
import resources.have_a_review_para
import resources.n_items_span
import resources.n_questions_dot_created_date_para
import resources.n_questions_in_dimension
import resources.n_questions_span
import resources.new_question_para
import resources.new_template_para
import resources.no_description_para
import resources.recently_created_para
import resources.recommended_for_you_para
import resources.x_and_n_more_para

data class OptionView(
    val title: @Composable () -> String,
    val preview: @Composable () -> String,
)

fun createOptionView(model: PractisoOption): OptionView =
    when (model) {
        is QuizOption -> OptionView(
            title = {
                model.quiz.name?.takeIf(String::isNotEmpty)
                    ?: stringResource(Res.string.new_question_para)
            },
            preview = {
                model.preview ?: stringResource(Res.string.empty_span)
            }
        )

        is SessionOption -> OptionView(
            title = { model.session.name },
            preview = {
                pluralStringResource(
                    Res.plurals.n_questions_dot_created_date_para,
                    model.quizCount,
                    model.quizCount,
                    HumanReadable.timeAgo(model.session.creationTimeISO)
                )
            }
        )

        is TemplateOption -> OptionView(
            title = {
                model.template.name.takeIf(String::isNotEmpty)
                    ?: stringResource(Res.string.new_template_para)
            },
            preview = {
                model.template.description ?: stringResource(Res.string.no_description_para)
            }
        )

        is SessionCreator -> createSessionCreatorView(model)
    }

fun createSessionCreatorView(model: SessionCreator): OptionView =
    when (model) {
        is DimensionOption -> OptionView(
            title = { model.dimension.name },
            preview = {
                if (model.quizCount > 0)
                    pluralStringResource(
                        Res.plurals.n_questions_span,
                        model.quizCount,
                        model.quizCount
                    )
                else stringResource(Res.string.empty_span)
            }
        )

        is SessionCreator.RecentlyCreatedQuizzes ->
            OptionView(
                title = { stringResource(Res.string.recently_created_para) },
                preview = {
                    val leading =
                        model.leadingQuizName ?: stringResource(Res.string.new_question_para)
                    if (model.selection.quizIds.size > 2) {
                        stringResource(
                            Res.string.x_and_n_more_para,
                            leading,
                            model.selection.quizIds.size - 1
                        )
                    } else {
                        leading
                    }
                }
            )

        is SessionCreator.RecentlyCreatedDimension ->
            OptionView(
                title = { stringResource(Res.string.recently_created_para) },
                preview = {
                     pluralStringResource(
                        Res.plurals.n_questions_in_dimension,
                        model.quizCount,
                        model.quizCount,
                         model.dimensionName
                    )
                }
            )

        is SessionCreator.FailMuch ->
            OptionView(
                title = { stringResource(Res.string.recommended_for_you_para) },
                preview = {
                    val itemName = model.leadingItemName
                    when {
                        itemName != null && model.itemCount > 1 ->
                            stringResource(
                                Res.string.x_and_n_more_para,
                                itemName,
                                model.itemCount - 1
                            )

                        itemName != null -> itemName
                        else -> pluralStringResource(Res.plurals.n_items_span, model.itemCount, model.itemCount)
                    }
                }
            )

        is SessionCreator.LeastAccessed ->
            OptionView(
                title = { stringResource(Res.string.have_a_review_para) },
                preview = {
                    val itemName = model.leadingItemName
                    when {
                        itemName != null && model.itemCount > 1 ->
                            stringResource(
                                Res.string.x_and_n_more_para,
                                itemName,
                                model.itemCount
                            )

                        itemName != null -> itemName
                        else -> pluralStringResource(Res.plurals.n_items_span, model.itemCount, model.itemCount)
                    }
                }
            )
    }
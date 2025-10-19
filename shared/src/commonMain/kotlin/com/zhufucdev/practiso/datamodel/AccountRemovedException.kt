package com.zhufucdev.practiso.datamodel

import kotlinx.coroutines.channels.Channel

data class AccountRemovedException(
    private val actionDuplex: Channel<Action>,
    override val cause: Throwable? = null
) :
    InteractiveException, AppException, Exception("Access denial: account removed") {

    override val scope: AppScope
        get() = AppScope.CommunityService

    override val appMessage: AppMessage
        get() = AppMessage.AccountRemoved

    override fun sendPrimary() {
        actionDuplex.trySend(ActionSignOff)
    }

    override fun sendSecondary() {
        actionDuplex.trySend(ActionCancel)
    }

    override val primaryActionLabel: ActionLabel
        get() = ActionLabel.ClearToken

    override val secondaryActionLabel: ActionLabel
        get() = ActionLabel.Cancel

    sealed class Action
    data object ActionCancel : Action()
    data object ActionSignOff : Action()
}

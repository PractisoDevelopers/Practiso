package com.zhufucdev.practiso.datamodel

interface InteractiveException {
    fun sendPrimary()
    fun sendSecondary()
    val primaryActionLabel: ActionLabel get() = ActionLabel.Confirm
    val secondaryActionLabel: ActionLabel get() = ActionLabel.Cancel
}

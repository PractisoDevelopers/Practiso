@file:Suppress("FunctionName")

package com.zhufucdev.practiso.bridge

import opacity.SetField

fun SetFieldUpdateString(value: String) = SetField.Update(value)
fun SetFieldUpdateStringNil() = SetField.Update<String?>(null)
fun SetFieldUnchangedString() = SetField.Unchanged<String>()
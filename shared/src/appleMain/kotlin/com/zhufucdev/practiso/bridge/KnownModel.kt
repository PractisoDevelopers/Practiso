package com.zhufucdev.practiso.bridge

import com.zhufucdev.practiso.KnownModels
import com.zhufucdev.practiso.datamodel.MlModel

fun KnownModel(index: Int): MlModel = KnownModels[index]
fun KnownModel(hfId: String): MlModel? = KnownModels[hfId]
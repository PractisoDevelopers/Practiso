package com.zhufucdev.practiso.bridge

import com.zhufucdev.practiso.datamodel.QuizDocument

fun Dictionary(quizDoc: QuizDocument) =
    quizDoc.resourcePool.mapValues { (_, loader) -> Data(loader()) }
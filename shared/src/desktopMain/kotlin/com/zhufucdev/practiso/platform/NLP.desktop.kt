package com.zhufucdev.practiso.platform

import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import com.zhufucdev.practiso.datamodel.MlModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

typealias LinguaLang = com.github.pemistahl.lingua.api.Language


actual class LanguageIdentifier {
    private val detector = LanguageDetectorBuilder.fromLanguages(
        LinguaLang.ENGLISH, LinguaLang.CHINESE, LinguaLang.SPANISH, LinguaLang.GERMAN
    ).build()

    actual suspend fun getLanguage(text: String): Language {
        val lang = detector.detectLanguageOf(text)
        return when (lang) {
            LinguaLang.ENGLISH -> Language.English
            LinguaLang.CHINESE -> Language.Chinese
            LinguaLang.SPANISH -> Language.Spanish
            LinguaLang.GERMAN -> Language.German
            LinguaLang.UNKNOWN -> Language.World
            else -> error("Unexpected language detection: ${lang.name}")
        }
    }
}

actual fun createFrameEmbeddingInference(model: MlModel, session: InferenceSession): Flow<InferenceModelState> = flow {
    // TODO
}

actual class InferenceSession {
    actual companion object {
        actual val default: InferenceSession = InferenceSession()
    }
}
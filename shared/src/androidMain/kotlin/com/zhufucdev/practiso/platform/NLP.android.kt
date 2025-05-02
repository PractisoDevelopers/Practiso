package com.zhufucdev.practiso.platform

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.zhufucdev.practiso.datamodel.MlModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun Language(bcp47Code: String): Language {
    if (bcp47Code.startsWith("en")) {
        return Language.English
    }
    if (bcp47Code.startsWith("zh")) {
        return Language.Chinese
    }
    if (bcp47Code.startsWith("de")) {
        return Language.German
    }
    if (bcp47Code.startsWith("es")) {
        return Language.Spanish
    }
    return Language.World
}

actual class LanguageIdentifier {
    actual suspend fun getLanguage(text: String): Language = suspendCancellableCoroutine { c ->
        LanguageIdentification.getClient()
            .identifyLanguage(text)
            .addOnSuccessListener {
                c.resume(Language(it))
            }
            .addOnFailureListener {
                c.resumeWithException(it)
            }
            .addOnCanceledListener {
                c.cancel()
            }
    }
}

actual suspend fun FrameEmbeddingInference(model: MlModel): FrameEmbeddingInference {
    TODO("Not yet implemented")
}
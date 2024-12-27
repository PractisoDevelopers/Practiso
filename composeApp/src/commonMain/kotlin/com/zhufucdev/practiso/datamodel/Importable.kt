package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.platform.source
import io.github.vinceglb.filekit.core.PlatformFile
import okio.Source

data class Importable(val name: String, val source: Source) {
    companion object {
        suspend fun fromFile(file: PlatformFile) = Importable(file.name, file.source())
    }
}


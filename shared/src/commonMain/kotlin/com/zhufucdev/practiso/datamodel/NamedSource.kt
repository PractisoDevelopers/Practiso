package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.platform.source
import io.github.vinceglb.filekit.core.PlatformFile
import okio.Source

data class NamedSource(val name: String, val source: Source) {
    companion object {
        suspend fun fromFile(file: PlatformFile) = NamedSource(file.name, file.source())
    }
}


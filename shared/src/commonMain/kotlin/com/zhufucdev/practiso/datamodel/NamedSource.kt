package com.zhufucdev.practiso.datamodel

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.source
import kotlinx.io.okio.asOkioSource
import okio.Source

data class NamedSource(val name: String, val source: Source) {
    companion object {
        fun fromFile(file: PlatformFile) = NamedSource(file.name, file.source().asOkioSource())
    }
}

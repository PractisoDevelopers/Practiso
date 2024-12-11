package com.zhufucdev.practiso.composable

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.zhufucdev.practiso.platform.BitmapLoader
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.buffer
import org.jetbrains.compose.resources.painterResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_image
import practiso.composeapp.generated.resources.baseline_image_remove_outline

data class BitmapRepository(val map: MutableMap<Path, ImageBitmap> = mutableMapOf())

@Stable
class FileImageState {
    var image: ImageLoaderState by mutableStateOf(ImageLoaderState.Pending)
}

sealed interface ImageLoaderState {
    data object Pending : ImageLoaderState
    data class Loaded(val bitmap: ImageBitmap, val path: Path) : ImageLoaderState
    data object Corrupt : ImageLoaderState
}

@Composable
fun rememberFileImageState() = remember { FileImageState() }

@Composable
fun FileImage(
    path: Path?,
    contentDescription: String?,
    cache: BitmapRepository = remember { BitmapRepository() },
    fileSystem: FileSystem = getPlatform().filesystem,
    state: FileImageState = rememberFileImageState(),
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    LaunchedEffect(path) {
        if (path == null) {
            state.image = ImageLoaderState.Pending
            return@LaunchedEffect
        }

        val imState = state.image
        if (imState !is ImageLoaderState.Loaded || imState.path != path) {
            val bm = withContext(Dispatchers.IO) {
                runCatching {
                    cache.map.getOrPut(path) {
                        path.takeIf { fileSystem.exists(it) }
                            ?.let { fileSystem.source(it) }
                            ?.buffer()
                            ?.readByteArray()
                            ?.let(BitmapLoader::from)
                            ?: return@withContext null
                    }
                }.getOrNull()
            }
            state.image = if (bm == null) {
                ImageLoaderState.Corrupt
            } else {
                ImageLoaderState.Loaded(bm, path)
            }
        }
    }

    when (val s = state.image) {
        ImageLoaderState.Corrupt -> Icon(
            painterResource(Res.drawable.baseline_image_remove_outline),
            contentDescription, modifier
        )

        is ImageLoaderState.Loaded ->
            Image(
                bitmap = s.bitmap,
                contentDescription, modifier, alignment, contentScale, alpha, colorFilter
            )

        ImageLoaderState.Pending -> Icon(
            painterResource(Res.drawable.baseline_image),
            contentDescription, modifier
        )
    }
}
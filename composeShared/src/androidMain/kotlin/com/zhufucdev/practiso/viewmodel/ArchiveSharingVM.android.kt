package com.zhufucdev.practiso.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.ForActivityResultLaunchable
import com.zhufucdev.practiso.SharedContext
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.AppCommunityService
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.ExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.gzip
import java.io.File

actual class ArchiveSharingViewModel(
    db: AppDatabase,
    exportService: ExportService,
    communityService: Flow<CommunityService>,
) :
    CommonArchiveSharingViewModel(db, exportService, communityService) {
    private val _shareWithOtherApps = Channel<ForActivityResultLaunchable>()
    val shareWithOtherApps: SendChannel<ForActivityResultLaunchable> get() = _shareWithOtherApps

    var systemShareState: SystemShare? by mutableStateOf(null)
        private set

    sealed class SystemShare {
        data class NamedRequired(val submission: SendChannel<String>) : SystemShare()
        data object Sending : SystemShare()
        data object Cancelled : SystemShare()
        data object Success : SystemShare()
    }

    override fun cancel() {
        super.cancel()
        systemShareState = null
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                select {
                    _shareWithOtherApps.onReceive { activityResult ->
                        launchToJobPool {
                            val nameDuplexChannel = Channel<String>()
                            systemShareState = SystemShare.NamedRequired(nameDuplexChannel)
                            val archiveName = nameDuplexChannel.receive()
                            val fileName = "share_archive_temp/$archiveName.psarchive"
                            val file = File(SharedContext.filesDir, fileName)
                            if (file.parentFile?.exists() == false) {
                                file.parentFile!!.mkdirs()
                            }

                            systemShareState = SystemShare.Sending
                            withContext(Dispatchers.IO) {
                                val sink =
                                    getPlatform().filesystem.sink(file.toOkioPath())
                                        .gzip()
                                        .buffer()
                                val source = exportService.exportAsSource(selection)
                                sink.writeAll(source)
                                sink.close()
                                source.close()
                            }

                            val fileUri = FileProvider.getUriForFile(
                                SharedContext,
                                "${SharedContext.packageName}.share.archive",
                                file
                            )
                            val shareIntent = ShareCompat.IntentBuilder(SharedContext)
                                .setType("application/gzip")
                                .setStream(fileUri)
                                .setText(archiveName)
                                .intent
                                .apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            val result = activityResult.startActivityForResult(
                                Intent.createChooser(
                                    shareIntent,
                                    null
                                )
                            )
                            systemShareState =
                                if (result.resultCode == Activity.RESULT_OK) SystemShare.Success else SystemShare.Cancelled
                        }
                    }
                }
            }
        }
    }

    actual companion object Companion {
        actual val Factory: ViewModelProvider.Factory
            get() = viewModelFactory {
                initializer {
                    ArchiveSharingViewModel(
                        Database.app, ExportService(Database.app),
                        AppCommunityService
                    )
                }
            }
    }
}
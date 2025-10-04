package com.zhufucdev.practiso.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.service.AppCommunityService
import com.zhufucdev.practiso.service.CommunityService
import com.zhufucdev.practiso.service.ExportService
import kotlinx.coroutines.flow.Flow

actual class ArchiveSharingViewModel(
    db: AppDatabase,
    exportService: ExportService,
    communityService: Flow<CommunityService>,
) :
    CommonArchiveSharingViewModel(db, exportService, communityService) {
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
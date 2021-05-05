package com.openlattice.chronicle.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager

class UploadStatusModel : ViewModel() {
    // WorkInfo for upload worker
    internal val outputWorkInfo: LiveData<List<WorkInfo>> = WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData("upload")
}
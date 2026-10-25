package com.jackson4rocks.evexia

import android.app.Application
import android.util.Log
import androidx.work.WorkManager
import com.jackson4rocks.evexia.notifications.Notifications
import java.io.File

class EvexiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        WorkManager.getInstance(this).cancelUniqueWork(LEGACY_MODEL_DOWNLOAD_WORK)
        listOf(
            File(filesDir, "evexia-models/Llama-3.2-1B-Instruct-Q4_K_M.gguf"),
            File(filesDir, "evexia-models/Llama-3.2-1B-Instruct-Q4_K_M.gguf.part"),
        ).forEach { modelFile ->
            if (modelFile.exists() && !modelFile.delete()) {
                Log.w(TAG, "Couldn't remove the obsolete on-device AI model at ${modelFile.name}.")
            }
        }
    }

    private companion object {
        const val TAG = "EvexiaApp"
        const val LEGACY_MODEL_DOWNLOAD_WORK = "evexia_local_model_download"
    }
}

package com.jackson4rocks.evexia.util

import android.content.Context
import java.io.File
import java.io.IOException
import java.security.KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LegacyGroqCredentialCleanup {
    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        val credentialFile = File(context.noBackupFilesDir, "evexia_groq_api_key")
        if (credentialFile.exists() && !credentialFile.delete()) {
            throw IOException("Couldn't remove the previous AI credential.")
        }
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias("evexia_groq_api_key")) {
            keyStore.deleteEntry("evexia_groq_api_key")
        }
    }
}

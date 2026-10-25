package com.jackson4rocks.evexia.util

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object FileStore {
    fun documentsDir(context: Context): File =
        File(context.filesDir, "documents").apply { mkdirs() }

    fun imagesDir(context: Context): File =
        File(context.filesDir, "images").apply { mkdirs() }

    fun profileDir(context: Context): File =
        File(context.filesDir, "profile").apply { mkdirs() }

    fun safeName(raw: String, fallback: String = "document"): String {
        val cleaned = raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_")
            .replace(Regex("\\s+"), " ")
            .take(90)
            .trim('.', ' ')
        return cleaned.ifBlank { fallback }
    }

    fun uniqueFile(directory: File, baseName: String, extension: String): File {
        val ext = extension.removePrefix(".").lowercase(Locale.US)
        var candidate = File(directory, if (ext.isBlank()) baseName else "$baseName.$ext")
        var index = 2
        while (candidate.exists()) {
            candidate = File(directory, if (ext.isBlank()) "$baseName $index" else "$baseName $index.$ext")
            index++
        }
        return candidate
    }

    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return uri.lastPathSegment ?: "File"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "File"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "File"
    }

    fun mimeType(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.US).orEmpty(),
            ) ?: "application/octet-stream"
        }
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    fun sizeOf(context: Context, uri: Uri): Long {
        if (uri.scheme == "file") return uri.path?.let { File(it).length() } ?: 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getLong(idx)
        }
        return 0L
    }

    fun extensionOf(name: String, mime: String): String {
        val fromName = name.substringAfterLast('.', "").lowercase(Locale.US).take(8)
        if (fromName.isNotBlank()) return fromName
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty()
    }

    suspend fun importDocument(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val original = displayName(context, uri)
            val mime = mimeType(context, uri)
            val ext = extensionOf(original, mime)
            val base = original.substringBeforeLast('.', original).ifBlank { "document" }
            val destination = uniqueFile(documentsDir(context), safeName(base), ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output, 256 * 1024) }
            } ?: return@runCatching null
            destination
        }.getOrNull()
    }

    suspend fun copyImageToCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = mimeType(context, uri)
            val ext = extensionOf(displayName(context, uri), mime).ifBlank { "jpg" }
            val destination = uniqueFile(File(context.cacheDir, "scans").apply { mkdirs() }, "page", ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output, 256 * 1024) }
            } ?: return@runCatching null
            destination
        }.getOrNull()
    }

    suspend fun copyDocumentToCache(context: Context, uri: Uri, prefix: String = "file"): File? = withContext(Dispatchers.IO) {
        runCatching {
            val original = displayName(context, uri)
            val mime = mimeType(context, uri)
            val ext = extensionOf(original, mime)
            val base = original.substringBeforeLast('.').ifBlank { prefix }
            // Keep the user's name available in print previews and document lists.
            val destination = uniqueFile(File(context.cacheDir, "converter").apply { mkdirs() }, safeName(base), ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output, 256 * 1024) }
            } ?: return@runCatching null
            destination
        }.getOrNull()
    }

    fun clearFlowAiCache(context: Context) {
        File(context.cacheDir, "flow_ai").listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }
    }

    suspend fun copyToFlowAiCache(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        val original = runCatching { displayName(context, uri) }.getOrDefault("attachment")
        val mime = runCatching { mimeType(context, uri) }.getOrDefault("application/octet-stream")
        val ext = extensionOf(original, mime)
        val base = original.substringBeforeLast('.').ifBlank { "attachment" }
        val destination = uniqueFile(
            File(context.cacheDir, "flow_ai").apply { mkdirs() },
            safeName(base),
            ext,
        )
        val input = try {
            context.contentResolver.openInputStream(uri)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            null
        }
        if (input == null) {
            destination.delete()
            return@withContext null
        }
        try {
            input.use { stream ->
                FileOutputStream(destination).use { output -> stream.copyTo(output, 256 * 1024) }
            }
        } catch (cancel: CancellationException) {
            destination.delete()
            throw cancel
        } catch (_: Exception) {
            destination.delete()
            return@withContext null
        }
        destination
    }

    suspend fun copyProfileImage(context: Context, uri: Uri, prefix: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = mimeType(context, uri)
            val ext = extensionOf(displayName(context, uri), mime).ifBlank { "jpg" }
            val destination = uniqueFile(profileDir(context), "${prefix}_${System.currentTimeMillis()}", ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output, 512 * 1024) }
            } ?: return@runCatching null
            destination
        }.getOrNull()
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun open(context: Context, location: String, mime: String) {
        val file = File(location)
        if (!file.exists()) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, file), mime.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: ActivityNotFoundException) {
            // The caller surfaces a friendly error when no capable app exists.
        }
    }

    fun share(context: Context, location: String, mime: String) {
        val file = File(location)
        if (!file.exists()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share document"))
    }

    fun exportPdfToDownloads(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Evexia")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            }.getOrDefault(false)
        }
        return runCatching {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(downloads, "Evexia").apply { mkdirs() }
            file.copyTo(uniqueFile(folder, file.nameWithoutExtension, "pdf"), overwrite = false)
            true
        }.getOrDefault(false)
    }

    fun humanSize(bytes: Long): String {
        if (bytes <= 0) return "—"
        val kb = bytes / 1024.0
        return when {
            kb < 1024 -> "${kb.toInt().coerceAtLeast(1)} KB"
            kb < 1024 * 1024 -> String.format(Locale.US, "%.1f MB", kb / 1024)
            else -> String.format(Locale.US, "%.1f GB", kb / (1024 * 1024))
        }
    }

    fun directorySize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun isUrl(location: String): Boolean =
        location.startsWith("http://") || location.startsWith("https://")

    fun deleteFile(location: String) {
        if (!isUrl(location)) runCatching { File(location).delete() }
    }
}

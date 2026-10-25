package com.jackson4rocks.evexia.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.data.NoteEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class FlowAiDocument(
    val title: String,
    val text: String,
    val truncated: Boolean = false,
) {
    val promptText: String
        get() = buildString {
            append("Document title: ")
            append(title)
            append("\n\n")
            append(text)
            if (truncated) {
                append(
                    "\n\n[The source was shortened to keep this request manageable. " +
                        "Summarise what is available.]",
                )
            }
        }
}

class FlowAiDocumentException(message: String) : IOException(message)

/**
 * Reads the document formats Evexia can create or import. Text PDFs use
 * PDFBox; image-only PDFs, scans and images fall back to on-device OCR.
 */
object FlowAiDocuments {
    private const val MAX_CHARS = 120_000
    private const val MAX_SOURCE_BYTES = 150L * 1024 * 1024
    private const val MAX_PDF_TEXT_BYTES = 24L * 1024 * 1024
    private const val MAX_PDF_MAIN_MEMORY = 16L * 1024 * 1024
    private const val MAX_OCR_PAGES = 12
    private const val OCR_TARGET_WIDTH = 1_500
    private const val MAX_OCR_HEIGHT = 3_000
    private const val MAX_IMAGE_EDGE = 2_000
    private const val OCR_MIN_TEXT = 30

    @Volatile
    private var pdfBoxReady = false

    fun fromNote(note: NoteEntity): FlowAiDocument {
        val body = note.content.trim()
        if (body.isBlank()) {
            throw FlowAiDocumentException("That note is empty. Add some text first.")
        }
        val clipped = body.take(MAX_CHARS)
        return FlowAiDocument(
            title = note.title.ifBlank { "Untitled note" },
            text = clipped,
            truncated = body.length > MAX_CHARS,
        )
    }

    fun supports(item: LibraryItemEntity): Boolean =
        item.kind != LibraryItemEntity.KIND_BOOKMARK && kindOf(item) != Kind.UNSUPPORTED

    fun kindLabel(item: LibraryItemEntity): String = when (kindOf(item)) {
        Kind.PDF -> "PDF"
        Kind.TEXT -> "Text"
        Kind.IMAGE -> "Image"
        Kind.UNSUPPORTED -> "Unsupported"
    }

    suspend fun fromLibraryItem(
        context: Context,
        item: LibraryItemEntity,
        onProgress: (String) -> Unit = {},
    ): FlowAiDocument = withContext(Dispatchers.IO) {
        val file = File(item.location)
        if (!file.exists()) {
            throw FlowAiDocumentException("That file is no longer available.")
        }
        if (file.length() > MAX_SOURCE_BYTES) {
            throw FlowAiDocumentException(
                "That file is too large to read on this phone. Try a smaller file.",
            )
        }
        fromFile(context, file, item.title, item.mimeType, onProgress)
    }

    /**
     * Copies a photo or file chosen from the system picker into a private cache
     * file, reads it, then removes the copy. The URI itself is never retained.
     */
    suspend fun fromPickedUri(
        context: Context,
        uri: Uri,
        onProgress: (String) -> Unit = {},
    ): FlowAiDocument = withContext(Dispatchers.IO) {
        val title = runCatching { FileStore.displayName(context, uri) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Attached file"
        val mime = runCatching { FileStore.mimeType(context, uri) }
            .getOrNull()
            ?: "application/octet-stream"
        val declaredSize = runCatching { FileStore.sizeOf(context, uri) }.getOrDefault(0L)
        if (declaredSize > MAX_SOURCE_BYTES) {
            throw FlowAiDocumentException(
                "That file is too large to read on this phone. Try a smaller file.",
            )
        }

        val cached = FileStore.copyToFlowAiCache(context, uri)
            ?: throw FlowAiDocumentException("I couldn't open that photo or file.")
        try {
            if (cached.length() > MAX_SOURCE_BYTES) {
                throw FlowAiDocumentException(
                    "That file is too large to read on this phone. Try a smaller file.",
                )
            }
            fromFile(context, cached, title, mime, onProgress)
        } finally {
            runCatching { cached.delete() }
        }
    }

    private suspend fun fromFile(
        context: Context,
        file: File,
        title: String,
        mime: String,
        onProgress: (String) -> Unit,
    ): FlowAiDocument = when (kindOf(title, mime)) {
        Kind.PDF -> fromPdf(context, file, title, onProgress)
        Kind.TEXT -> fromTextFile(file, title)
        Kind.IMAGE -> fromImage(file, title, onProgress)
        Kind.UNSUPPORTED -> throw FlowAiDocumentException(
            "Evexia AI can read photos, PDFs and plain text files.",
        )
    }

    private suspend fun fromPdf(
        context: Context,
        file: File,
        title: String,
        onProgress: (String) -> Unit,
    ): FlowAiDocument {
        onProgress("Reading the PDF…")
        val extracted = if (file.length() <= MAX_PDF_TEXT_BYTES) {
            try {
                extractPdfText(context, file)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: OutOfMemoryError) {
                throw error
            } catch (_: Exception) {
                ""
            }
        } else {
            ""
        }.trim()
        var bestText = extracted
        var usedOcr = false

        if (bestText.length < OCR_MIN_TEXT) {
            onProgress("Looking for text in the pages…")
            val ocrText = try {
                ocrPdf(file, onProgress)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: OutOfMemoryError) {
                throw error
            } catch (error: Exception) {
                if (bestText.isBlank()) {
                    throw FlowAiDocumentException(
                        error.message ?: "I couldn't read text from that PDF.",
                    )
                }
                ""
            }
            if (ocrText.length > bestText.length) {
                bestText = ocrText
                usedOcr = true
            }
        }

        val cleaned = normalise(bestText)
        if (cleaned.isBlank()) {
            throw FlowAiDocumentException(
                "I couldn't find readable text in that PDF. If it is a scan, try a clearer copy.",
            )
        }
        val clipped = cleaned.take(MAX_CHARS)
        return FlowAiDocument(
            title = title.ifBlank { file.name },
            text = clipped,
            truncated = cleaned.length > MAX_CHARS || (usedOcr && pdfPageCount(file) > MAX_OCR_PAGES),
        )
    }

    private fun fromTextFile(file: File, title: String): FlowAiDocument {
        val raw = try {
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(MAX_CHARS + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = reader.read(buffer, total, buffer.size - total)
                    if (read < 0) break
                    total += read
                }
                String(buffer, 0, total)
            }
        } catch (error: OutOfMemoryError) {
            throw error
        } catch (_: Exception) {
            throw FlowAiDocumentException("I couldn't open that text file.")
        }
        val cleaned = normalise(raw)
        if (cleaned.isBlank()) {
            throw FlowAiDocumentException("That text file is empty.")
        }
        return FlowAiDocument(
            title = title.ifBlank { file.name },
            text = cleaned.take(MAX_CHARS),
            truncated = raw.length > MAX_CHARS || cleaned.length > MAX_CHARS,
        )
    }

    private suspend fun fromImage(
        file: File,
        title: String,
        onProgress: (String) -> Unit,
    ): FlowAiDocument {
        onProgress("Reading the image…")
        val bitmap = decodeSampled(file)
            ?: throw FlowAiDocumentException("I couldn't open that image.")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val raw = try {
            recognise(recognizer, bitmap)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            throw FlowAiDocumentException(
                error.message ?: "I couldn't read text from that image.",
            )
        } finally {
            recognizer.close()
        }
        val cleaned = normalise(raw)
        if (cleaned.isBlank()) {
            throw FlowAiDocumentException("I couldn't find readable text in that image.")
        }
        return FlowAiDocument(
            title = title.ifBlank { file.name },
            text = cleaned.take(MAX_CHARS),
            truncated = cleaned.length > MAX_CHARS,
        )
    }

    private fun extractPdfText(context: Context, file: File): String {
        initPdfBox(context)
        val memory = MemoryUsageSetting
            .setupMixed(MAX_PDF_MAIN_MEMORY)
            .setTempDir(context.cacheDir)
        return PDDocument.load(file, memory).use { document ->
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            val text = StringBuilder()
            for (page in 1..document.numberOfPages) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(document)
                if (pageText.isNotBlank()) {
                    if (text.isNotEmpty()) text.append("\n\n")
                    text.append(pageText)
                }
                if (text.length >= MAX_CHARS + 500) break
            }
            text.toString()
        }
    }

    private suspend fun ocrPdf(
        file: File,
        onProgress: (String) -> Unit,
    ): String {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val pages = StringBuilder()
        try {
            PdfRenderer(descriptor).use { renderer ->
                val count = min(renderer.pageCount, MAX_OCR_PAGES)
                if (count == 0) throw FlowAiDocumentException("That PDF has no pages.")
                for (index in 0 until count) {
                    onProgress("Scanning page ${index + 1} of $count…")
                    val pageText = try {
                        renderer.openPage(index).use { page ->
                            val width = page.width.coerceAtLeast(1)
                            val height = page.height.coerceAtLeast(1)
                            val scale = minOf(
                                OCR_TARGET_WIDTH.toFloat() / width,
                                MAX_OCR_HEIGHT.toFloat() / height,
                                3f,
                            )
                            val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
                            val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(
                                targetWidth,
                                targetHeight,
                                Bitmap.Config.ARGB_8888,
                            )
                            var handedToOcr = false
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                handedToOcr = true
                                recognise(recognizer, bitmap)
                            } finally {
                                if (!handedToOcr && !bitmap.isRecycled) bitmap.recycle()
                            }
                        }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        ""
                    }
                    if (pageText.isNotBlank()) {
                        if (pages.isNotEmpty()) pages.append("\n\n")
                        pages.append(pageText.trim())
                    }
                }
            }
        } finally {
            recognizer.close()
            runCatching { descriptor.close() }
        }
        return pages.toString()
    }

    private suspend fun recognise(recognizer: TextRecognizer, bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            try {
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { result ->
                        bitmap.recycle()
                        if (continuation.isActive) continuation.resume(result.text)
                    }
                    .addOnFailureListener { error ->
                        bitmap.recycle()
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            } catch (error: Throwable) {
                bitmap.recycle()
                throw error
            }
        }

    private fun decodeSampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_EDGE) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

    private fun pdfPageCount(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        }
    }.getOrDefault(0)

    private fun initPdfBox(context: Context) {
        if (pdfBoxReady) return
        synchronized(this) {
            if (!pdfBoxReady) {
                PDFBoxResourceLoader.init(context.applicationContext)
                pdfBoxReady = true
            }
        }
    }

    private fun normalise(raw: String): String = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
        .replace(Regex("[ \\t]+\\n"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun kindOf(item: LibraryItemEntity): Kind = kindOf(item.title, item.mimeType)

    private fun kindOf(title: String, mimeType: String): Kind {
        val name = title.lowercase(Locale.US)
        val mime = mimeType.lowercase(Locale.US)
        return when {
            mime == "application/pdf" || name.endsWith(".pdf") -> Kind.PDF
            mime.startsWith("text/") || name.endsWith(".txt") ||
                name.endsWith(".md") || name.endsWith(".markdown") -> Kind.TEXT
            mime.startsWith("image/") || name.endsWith(".jpg") ||
                name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".webp") -> Kind.IMAGE
            else -> Kind.UNSUPPORTED
        }
    }

    private enum class Kind { PDF, TEXT, IMAGE, UNSUPPORTED }
}

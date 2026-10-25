package com.jackson4rocks.evexia.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Sends PDF files and images to Android's print framework. Printing stays
 * local: the selected print service receives the document, Evexia does not
 * upload anything.
 */
object Printer {

    fun isPrintable(file: File): Boolean {
        if (!file.isFile) return false
        return when (file.extension.lowercase(Locale.US)) {
            "pdf" -> true
            "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif" -> true
            else -> false
        }
    }

    fun print(
        context: Context,
        file: File,
        jobName: String = file.nameWithoutExtension,
        monochrome: Boolean = false,
    ): Result<Unit> =
        runCatching {
            require(file.isFile) { "That document is no longer available." }
            val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: error("Printing is not available on this device.")
            val extension = file.extension.lowercase(Locale.US)
            val adapter = when (extension) {
                "pdf" -> PdfFilePrintAdapter(file, monochrome)
                "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif" -> ImagePrintAdapter(file, monochrome)
                else -> error("Only PDF documents and images can be printed.")
            }
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setColorMode(
                    if (monochrome) PrintAttributes.COLOR_MODE_MONOCHROME
                    else PrintAttributes.COLOR_MODE_COLOR,
                )
                .build()
            manager.print(jobName.ifBlank { "Evexia document" }.take(80), adapter, attributes)
        }
}

private class PdfFilePrintAdapter(
    private val file: File,
    private val monochrome: Boolean,
) : PrintDocumentAdapter() {
    private var pageCount = 1

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        pageCount = countPages(file)
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        if (destination == null) {
            callback?.onWriteFailed("The print destination was unavailable.")
            return
        }
        val output = PdfDocument()
        try {
            val selected = selectedPages(pages, pageCount)
            if (selected.isEmpty()) {
                callback?.onWriteFailed("No pages were selected.")
                return
            }
            val allPages = selected.size == pageCount &&
                selected.first() == 0 && selected.last() == pageCount - 1
            // Colour PDFs are already print-ready: copy the source instead of
            // rasterising every page. This is the fast path for normal prints.
            if (!monochrome && allPages) {
                file.inputStream().use { input ->
                    FileOutputStream(destination.fileDescriptor).use { stream ->
                        input.copyTo(stream)
                        stream.flush()
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                return
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                if (monochrome) colorFilter = grayscaleFilter()
            }
            val source = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(source).use { renderer ->
                selected.forEachIndexed { outputIndex, pageIndex ->
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    renderer.openPage(pageIndex).use { page ->
                        val scale = (1800f / max(page.width, page.height).coerceAtLeast(1))
                            .coerceIn(1f, 3f)
                        val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                        val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, outputIndex + 1).create()
                        val target = output.startPage(pageInfo)
                        target.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                        output.finishPage(target)
                        bitmap.recycle()
                    }
                }
            }
            FileOutputStream(destination.fileDescriptor).use { stream ->
                output.writeTo(stream)
                stream.flush()
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Throwable) {
            runCatching { output.close() }
            callback?.onWriteFailed(error.message ?: "Couldn't prepare this PDF for printing.")
        } finally {
            runCatching { output.close() }
        }
    }

    private fun countPages(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        }
    }.getOrDefault(1)

    private fun selectedPages(ranges: Array<out PageRange>?, total: Int): List<Int> {
        if (ranges.isNullOrEmpty() || ranges.any { it.start == 0 && it.end == Int.MAX_VALUE }) {
            return (0 until total).toList()
        }
        val indices = linkedSetOf<Int>()
        ranges.forEach { range ->
            val start = range.start.coerceAtLeast(0)
            val end = range.end.coerceAtMost(total - 1)
            if (start <= end) {
                for (index in start..end) indices.add(index)
            }
        }
        return indices.sorted()
    }
}

private class ImagePrintAdapter(
    private val file: File,
    private val monochrome: Boolean,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(1)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        if (destination == null) {
            callback?.onWriteFailed("The print destination was unavailable.")
            return
        }
        var bitmap: Bitmap? = null
        val output = PdfDocument()
        try {
            bitmap = decodeForPrint(file)
                ?: error("Couldn't read that image.")
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                return
            }
            val landscape = bitmap.width > bitmap.height
            val pageWidth = if (landscape) 842 else 595
            val pageHeight = if (landscape) 595 else 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = output.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            val margin = 24f
            val scale = min(
                (pageWidth - margin * 2) / bitmap.width.toFloat(),
                (pageHeight - margin * 2) / bitmap.height.toFloat(),
            )
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val left = (pageWidth - drawWidth) / 2f
            val top = (pageHeight - drawHeight) / 2f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                if (monochrome) colorFilter = grayscaleFilter()
            }
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(left, top, left + drawWidth, top + drawHeight),
                paint,
            )
            output.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { stream ->
                output.writeTo(stream)
                stream.flush()
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Throwable) {
            callback?.onWriteFailed(error.message ?: "Couldn't prepare this image for printing.")
        } finally {
            bitmap?.recycle()
            runCatching { output.close() }
        }
    }

    private fun decodeForPrint(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val rotation = runCatching {
            when (
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (rotation == 0f) return decoded
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { rotated -> if (rotated !== decoded) decoded.recycle() }
    }
}

private fun grayscaleFilter(): ColorMatrixColorFilter =
    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

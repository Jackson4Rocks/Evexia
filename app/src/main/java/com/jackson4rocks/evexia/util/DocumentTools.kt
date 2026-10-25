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
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

enum class PdfPageSize(val label: String, val width: Int, val height: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
    ;

    companion object {
        fun from(value: String): PdfPageSize = entries.firstOrNull { it.name == value } ?: A4
    }
}

enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    ENHANCE("Enhance"),
    BLACK_AND_WHITE("B&W"),
    ;

    companion object {
        fun from(value: String): ScanFilter = entries.firstOrNull { it.name == value } ?: ENHANCE
    }
}

data class PdfCreationResult(
    val file: File,
    val pageCount: Int,
)

object DocumentTools {

    suspend fun imagesToPdf(
        context: Context,
        images: List<File>,
        baseName: String,
        pageSize: PdfPageSize = PdfPageSize.A4,
        filter: ScanFilter = ScanFilter.ENHANCE,
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(images.isNotEmpty()) { "Add at least one image." }
        val output = FileStore.uniqueFile(FileStore.documentsDir(context), FileStore.safeName(baseName), "pdf")
        val document = PdfDocument()
        try {
            images.forEachIndexed { index, file ->
                val bitmap = decodeForPdf(file)
                    ?: error("Could not read ${file.name}.")
                val processed = when (filter) {
                    ScanFilter.ORIGINAL -> bitmap
                    ScanFilter.ENHANCE -> enhance(bitmap)
                    ScanFilter.BLACK_AND_WHITE -> blackAndWhite(bitmap)
                }
                val pageInfo = PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, index + 1).create()
                val page = document.startPage(pageInfo)
                drawImagePage(page.canvas, processed, pageSize.width, pageSize.height)
                document.finishPage(page)
                if (processed !== bitmap) processed.recycle()
                bitmap.recycle()
            }
            output.outputStream().use(document::writeTo)
            PdfCreationResult(output, images.size)
        } catch (t: Throwable) {
            document.close()
            output.delete()
            throw t
        } finally {
            runCatching { document.close() }
        }
    }

    suspend fun textToPdf(
        context: Context,
        documentTitle: String,
        body: String,
        baseName: String,
        pageSize: PdfPageSize = PdfPageSize.A4,
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        val title = documentTitle.trim().ifBlank { "Untitled document" }
        val content = body.trim()
        require(content.isNotEmpty()) { "Write something to convert." }
        val output = FileStore.uniqueFile(FileStore.documentsDir(context), FileStore.safeName(baseName), "pdf")
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 58f
        val pageWidth = pageSize.width.toFloat()
        val pageHeight = pageSize.height.toFloat()
        var pageNumber = 1

        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val info = PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, pageNumber).create()
            return document.startPage(info) to document.startPage(info).canvas
        }

        // PdfDocument requires finishPage on the exact page object, so keep one
        // page open at a time and finish before drawing the next.
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, pageNumber).create(),
        )
        var canvas = page.canvas
        var y = margin

        fun finishCurrentPage() {
            paint.textSize = 8.5f
            paint.color = Color.rgb(132, 132, 128)
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText("Evexia", margin, pageHeight - 28f, paint)
            canvas.drawText(
                "$pageNumber",
                pageWidth - margin - 8f,
                pageHeight - 28f,
                paint,
            )
            document.finishPage(page)
        }

        fun startNextPage() {
            finishCurrentPage()
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(pageSize.width, pageSize.height, pageNumber).create(),
            )
            canvas = page.canvas
            y = margin
        }

        try {
            paint.color = Color.rgb(24, 25, 23)
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            paint.textSize = 25f
            val titleWidth = pageWidth - margin * 2
            val titleLines = wrapText(title, paint, titleWidth)
            titleLines.forEach { line ->
                if (y > pageHeight - margin - 30f) startNextPage()
                canvas.drawText(line, margin, y, paint)
                y += 31f
            }
            y += 8f
            paint.color = Color.rgb(204, 216, 125)
            canvas.drawRect(margin, y, margin + 46f, y + 3f, paint)
            y += 30f

            paint.color = Color.rgb(44, 45, 42)
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = 11.5f
            val lineHeight = 18.5f
            content.split('\n').forEach { paragraph ->
                if (paragraph.isBlank()) {
                    y += lineHeight * 0.7f
                    return@forEach
                }
                val lines = wrapText(paragraph, paint, pageWidth - margin * 2)
                lines.forEach { line ->
                    if (y > pageHeight - margin - 18f) startNextPage()
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
            }
            finishCurrentPage()
            output.outputStream().use(document::writeTo)
            PdfCreationResult(output, pageNumber)
        } catch (t: Throwable) {
            output.delete()
            throw t
        } finally {
            runCatching { document.close() }
        }
    }

    suspend fun mergePdfs(
        context: Context,
        pdfs: List<File>,
        baseName: String,
    ): PdfCreationResult = withContext(Dispatchers.IO) {
        require(pdfs.size >= 2) { "Choose at least two PDFs to merge." }
        val output = FileStore.uniqueFile(FileStore.documentsDir(context), FileStore.safeName(baseName), "pdf")
        val document = PdfDocument()
        var outputPage = 1
        try {
            pdfs.forEach { pdf ->
                ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        for (pageIndex in 0 until renderer.pageCount) {
                            renderer.openPage(pageIndex).use { source ->
                                val scale = 2f
                                val bitmap = Bitmap.createBitmap(
                                    max(1, (source.width * scale).toInt()),
                                    max(1, (source.height * scale).toInt()),
                                    Bitmap.Config.ARGB_8888,
                                )
                                bitmap.eraseColor(Color.WHITE)
                                source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                val page = document.startPage(
                                    PdfDocument.PageInfo.Builder(
                                        source.width,
                                        source.height,
                                        outputPage,
                                    ).create(),
                                )
                                val destination = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
                                page.canvas.drawBitmap(bitmap, null, destination, Paint(Paint.FILTER_BITMAP_FLAG))
                                document.finishPage(page)
                                bitmap.recycle()
                                outputPage++
                            }
                        }
                    }
                }
            }
            output.outputStream().use(document::writeTo)
            PdfCreationResult(output, outputPage - 1)
        } catch (t: Throwable) {
            output.delete()
            throw t
        } finally {
            runCatching { document.close() }
        }
    }

    suspend fun pdfToImages(
        context: Context,
        pdf: File,
        baseName: String,
    ): List<File> = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "exports/${FileStore.safeName(baseName)}").apply { mkdirs() }
        val outputs = mutableListOf<File>()
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { page ->
                        val scale = 2f
                        val bitmap = Bitmap.createBitmap(
                            max(1, (page.width * scale).toInt()),
                            max(1, (page.height * scale).toInt()),
                            Bitmap.Config.ARGB_8888,
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val out = FileStore.uniqueFile(directory, "${FileStore.safeName(baseName)}_page_${index + 1}", "png")
                        out.outputStream().use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        bitmap.recycle()
                        outputs += out
                    }
                }
            }
        }
        outputs
    }

    private fun drawImagePage(canvas: Canvas, bitmap: Bitmap, pageWidth: Int, pageHeight: Int) {
        canvas.drawColor(Color.WHITE)
        val margin = 22f
        val availableWidth = pageWidth - margin * 2
        val availableHeight = pageHeight - margin * 2
        val scale = min(availableWidth / bitmap.width, availableHeight / bitmap.height)
        val drawWidth = bitmap.width * scale
        val drawHeight = bitmap.height * scale
        val left = (pageWidth - drawWidth) / 2f
        val top = (pageHeight - drawHeight) / 2f
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + drawWidth, top + drawHeight),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        var remainder = text.trim()
        while (remainder.isNotEmpty()) {
            val count = paint.breakText(remainder, true, maxWidth, null).coerceAtLeast(1)
            var cut = count.coerceAtMost(remainder.length)
            val lastSpace = remainder.lastIndexOf(' ', startIndex = cut - 1)
            if (cut < remainder.length && lastSpace > 0) cut = lastSpace
            out += remainder.substring(0, cut).trimEnd()
            remainder = remainder.substring(cut).trimStart()
        }
        return out
    }

    private fun decodeForPdf(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val largest = max(bounds.outWidth, bounds.outHeight)
        while (largest / sample > 2800) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        return if (matrix.isIdentity) decoded else Bitmap.createBitmap(
            decoded,
            0,
            0,
            decoded.width,
            decoded.height,
            matrix,
            true,
        ).also { if (it !== decoded) decoded.recycle() }
    }

    private fun blackAndWhite(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val mat = ColorMatrix().apply { setSaturation(0f) }
        val contrast = 1.85f
        val translate = (-0.5f * contrast + 0.5f) * 255f + 12f
        mat.postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            source,
            0f,
            0f,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(mat)
            },
        )
        return output
    }

    private fun enhance(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val saturation = ColorMatrix().apply {
            setSaturation(0f)
        }
        val contrast = 1.18f
        val translate = (-0.5f * contrast + 0.5f) * 255f + 8f
        val tone = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        saturation.postConcat(tone)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(saturation)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}

package com.jackson4rocks.evexia.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.GlassSheet
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.Radii
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SegmentedControl
import com.jackson4rocks.evexia.ui.components.Spinner
import com.jackson4rocks.evexia.ui.components.subjectColor
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.DateUtils
import com.jackson4rocks.evexia.util.FileStore
import com.jackson4rocks.evexia.util.PdfCreationResult
import com.jackson4rocks.evexia.util.PdfPageSize
import com.jackson4rocks.evexia.util.Printer
import com.jackson4rocks.evexia.util.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ScannerScreen(
    state: AppUiState,
    busy: Boolean,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    onCreatePdf: (
        images: List<File>,
        name: String,
        pageSize: PdfPageSize,
        filter: ScanFilter,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = state.preferences
    var pagePaths by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val pages = remember(pagePaths) { pagePaths.map(::File) }
    var previewPath by rememberSaveable { mutableStateOf<String?>(null) }
    val preview = previewPath?.let(::File)
    var name by rememberSaveable { mutableStateOf("Scan ${DateUtils.shortDate(java.time.LocalDate.now())}") }
    var pageSize by rememberSaveable { mutableStateOf(prefs.pdfPageSize) }
    var filter by rememberSaveable { mutableStateOf(prefs.scanFilter) }
    var subjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var capturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedName by rememberSaveable { mutableStateOf<String?>(null) }
    var savedPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = capturePath?.let(::File)
        if (success && file != null && file.exists() && file.length() > 0) {
            pagePaths = pagePaths + file.absolutePath
            error = null
            savedName = null
            savedPath = null
        } else {
            file?.delete()
        }
        capturePath = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                adding = true
                val copied = uris.mapNotNull { FileStore.copyImageToCache(context, it) }
                pagePaths = pagePaths + copied.map { it.absolutePath }
                adding = false
                if (copied.isEmpty()) error = "Couldn't read those images."
                else {
                    savedName = null
                    savedPath = null
                }
            }
        }
    }

    fun takePhoto() {
        runCatching {
            val folder = File(context.cacheDir, "scans").apply { mkdirs() }
            val file = FileStore.uniqueFile(folder, "scan", "jpg")
            capturePath = file.absolutePath
            cameraLauncher.launch(FileStore.uriFor(context, file))
        }.onFailure {
            error = "No camera app is available on this device."
            capturePath?.let { File(it).delete() }
            capturePath = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Scanner",
            onBack = onBack,
            eyebrow = "Turn paper into a PDF",
            actions = {
                if (pages.isNotEmpty()) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(colors.glassQuiet)
                            .border(1.dp, colors.border, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "${pages.size} ${if (pages.size == 1) "PAGE" else "PAGES"}",
                            style = type.label.copy(color = colors.textSecondary, fontSize = 8.sp),
                        )
                    }
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            if (savedName != null) {
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    accentTint = true,
                    shape = Radii.hero,
                    contentPadding = PaddingValues(22.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = colors.onAccent, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        BasicText(
                            text = "PDF saved",
                            style = type.display.copy(color = colors.textPrimary, fontSize = 27.sp),
                        )
                        BasicText(
                            text = "$savedName is in your library, ready to open, share or print.",
                            style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                            modifier = Modifier.padding(top = 5.dp, start = 12.dp, end = 12.dp),
                        )
                        Spacer(Modifier.height(17.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            SecondaryButton(
                                label = "Scan another",
                                onClick = {
                                    savedName = null
                                    savedPath = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                label = "Print",
                                onClick = {
                                    savedPath?.let { path ->
                                        Printer.print(context, File(path), monochrome = prefs.printMonochrome)
                                            .onSuccess { error = null }
                                            .onFailure {
                                                error = it.message ?: "Couldn't open the print preview."
                                            }
                                    }
                                },
                                icon = Icons.Outlined.Print,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(9.dp))
                        PrimaryButton(
                            label = "Open library",
                            onClick = onOpenLibrary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                shape = Radii.hero,
                accentTint = pages.isEmpty(),
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(colors.glassQuiet)
                            .border(1.dp, colors.border, RoundedCornerShape(21.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    BasicText(
                        text = if (pages.isEmpty()) "Capture the first page" else "Add another page",
                        style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 16.sp),
                    )
                    BasicText(
                        text = "Straighten the page, fill the frame, and keep the light even. Evexia handles the rest.",
                        style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                        modifier = Modifier.widthIn(max = 300.dp).padding(top = 5.dp),
                    )
                    Spacer(Modifier.height(15.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        PrimaryButton(
                            label = "Take photo",
                            onClick = { takePhoto() },
                            icon = Icons.Outlined.CameraAlt,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            label = "From gallery",
                            onClick = { galleryLauncher.launch("image/*") },
                            icon = Icons.Outlined.PhotoLibrary,
                            modifier = Modifier.weight(1f),
                            enabled = !adding,
                        )
                    }
                    if (adding) {
                        Spacer(Modifier.height(11.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spinner(size = 15.dp, color = colors.accentInk)
                            Spacer(Modifier.width(7.dp))
                            BasicText("Preparing pages…", style = type.caption.copy(color = colors.textSecondary))
                        }
                    }
                    error?.let {
                        Spacer(Modifier.height(9.dp))
                        BasicText(
                            text = it,
                            style = type.caption.copy(color = colors.danger, textAlign = TextAlign.Center),
                        )
                    }
                }
            }

            if (pages.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = "Pages",
                        style = type.sectionTitle.copy(color = colors.textPrimary),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = "${pages.size} · in this order",
                        style = type.caption.copy(color = colors.textTertiary),
                    )
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        text = "Clear",
                        style = type.caption.copy(color = colors.danger, fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.dangerSoft)
                            .clickable { pagePaths = emptyList() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(9.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PagePadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(pages, key = { it.absolutePath }) { file ->
                        val pageIndex = pages.indexOf(file)
                        ScanThumbnail(
                            file = file,
                            index = pageIndex,
                            onPreview = { previewPath = file.absolutePath },
                            onRemove = { pagePaths = pagePaths.filterNot { it == file.absolutePath } },
                        )
                    }
                    item {
                        Column(
                            Modifier
                                .width(92.dp)
                                .height(124.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(colors.glassQuiet)
                                .border(1.dp, colors.border, RoundedCornerShape(15.dp))
                                .padding(0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            IconButton(
                                icon = Icons.Filled.Add,
                                contentDescription = "Add page",
                                onClick = { takePhoto() },
                                size = 38.dp,
                            )
                            Spacer(Modifier.height(6.dp))
                            BasicText(
                                "Add",
                                style = type.caption.copy(color = colors.textSecondary, fontSize = 10.sp),
                            )
                        }
                    }
                }
                if (pages.size > 1) {
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        BasicText(
                            text = "Tap a page to preview · × to remove",
                            style = type.caption.copy(color = colors.textTertiary, fontSize = 9.5.sp),
                            modifier = Modifier.padding(horizontal = PagePadding),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Column(Modifier.padding(horizontal = PagePadding)) {
                AppTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = "Document name",
                    placeholder = "e.g. Biology lecture notes",
                    imeAction = ImeAction.Done,
                )
                Spacer(Modifier.height(16.dp))
                BasicText(
                    "Page size",
                    style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                SegmentedControl(
                    items = PdfPageSize.entries.map { it.label },
                    selectedIndex = PdfPageSize.entries.indexOf(PdfPageSize.from(pageSize)),
                    onSelected = { pageSize = PdfPageSize.entries[it].name },
                )
                Spacer(Modifier.height(16.dp))
                BasicText(
                    "Finish",
                    style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                SegmentedControl(
                    items = ScanFilter.entries.map { it.label },
                    selectedIndex = ScanFilter.entries.indexOf(ScanFilter.from(filter)),
                    onSelected = { filter = ScanFilter.entries[it].name },
                )
                if (state.subjects.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    BasicText(
                        "Subject",
                        style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        item { AppChip("No subject", subjectId == null, onClick = { subjectId = null }) }
                        items(state.subjects, key = { it.id }) { subject ->
                            AppChip(
                                label = subject.name,
                                selected = subjectId == subject.id,
                                onClick = { subjectId = subject.id },
                                dotColor = subjectColor(subject),
                            )
                        }
                    }
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding, vertical = 11.dp)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                label = if (pages.isEmpty()) {
                    "Add a page to begin"
                } else "Create PDF · ${pages.size} ${if (pages.size == 1) "page" else "pages"}",
                onClick = {
                    if (name.trim().isBlank()) name = "Scan ${DateUtils.shortDate(java.time.LocalDate.now())}"
                    onCreatePdf(
                        pages,
                        name.trim(),
                        PdfPageSize.from(pageSize),
                        ScanFilter.from(filter),
                        subjectId,
                    ) { result ->
                        result.onSuccess {
                            savedName = it.file.name
                            savedPath = it.file.absolutePath
                            pagePaths = emptyList()
                        }.onFailure {
                            error = it.message ?: "Couldn't create the PDF."
                        }
                    }
                },
                icon = Icons.Outlined.PictureAsPdf,
                enabled = pages.isNotEmpty() && !busy && !adding,
                loading = busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    preview?.let { file ->
        GlassSheet(
            title = "Page preview",
            subtitle = "Check legibility before it becomes a PDF.",
            onDismiss = { previewPath = null },
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.glassQuiet),
                contentAlignment = Alignment.Center,
            ) {
                ScanPhoto(file, Modifier.fillMaxSize(), ContentScale.Fit)
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                label = "Remove this page",
                onClick = {
                    pagePaths = pagePaths.filterNot { it == file.absolutePath }
                    previewPath = null
                },
                icon = Icons.Filled.DeleteOutline,
                danger = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScanThumbnail(
    file: File,
    index: Int,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    Box(
        Modifier
            .width(92.dp)
            .height(124.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(colors.glassQuiet)
            .border(1.dp, colors.border, RoundedCornerShape(15.dp))
            .clickable(onClick = onPreview),
    ) {
        ScanPhoto(file, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxWidth()
                .height(27.dp)
                .align(Alignment.BottomCenter)
                .background(colors.scrim.copy(alpha = 0.55f)),
        )
        BasicText(
            text = "${index + 1}",
            style = type.label.copy(color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp),
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.BottomStart),
        )
        BasicText(
            text = "VIEW",
            style = type.label.copy(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), fontSize = 7.sp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
        )
        IconButton(
            icon = Icons.Filled.Close,
            contentDescription = "Remove page",
            onClick = onRemove,
            size = 24.dp,
            glass = false,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .padding(5.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(colors.scrim.copy(alpha = 0.55f)),
        )
    }
}


@Composable
private fun ScanPhoto(
    file: File,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

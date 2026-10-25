package com.jackson4rocks.evexia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
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
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

private data class ConversionOutcome(
    val title: String,
    val detail: String,
    val file: File? = null,
    val pages: List<File> = emptyList(),
)

@Composable
fun ConverterScreen(
    state: AppUiState,
    busy: Boolean,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    onCreateTextPdf: (
        title: String,
        body: String,
        name: String,
        pageSize: PdfPageSize,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) -> Unit,
    onCreateImagePdf: (
        images: List<File>,
        name: String,
        pageSize: PdfPageSize,
        filter: ScanFilter,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) -> Unit,
    onMergePdfs: (
        files: List<File>,
        name: String,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) -> Unit,
    onPdfToImages: (
        file: File,
        name: String,
        onComplete: (Result<List<File>>) -> Unit,
    ) -> Unit,
    onSetPrintMonochrome: (Boolean) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = state.preferences
    val tools = listOf("Text", "Images", "Merge", "Print", "PDF → PNG")
    var tool by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var pageSize by rememberSaveable { mutableStateOf(prefs.pdfPageSize) }
    var filter by rememberSaveable { mutableStateOf(prefs.scanFilter) }
    var subjectId by rememberSaveable { mutableStateOf<Long?>(null) }
    var outcome by remember { mutableStateOf<ConversionOutcome?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }

    // Text
    var docTitle by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    // Images / merge / source are persisted as paths so rotation doesn't discard them.
    var imagePaths by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val images = remember(imagePaths) { imagePaths.map(::File) }
    var pdfPaths by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val pdfs = remember(pdfPaths) { pdfPaths.map(::File) }
    var sourcePath by rememberSaveable { mutableStateOf<String?>(null) }
    val sourcePdf = sourcePath?.let(::File)
    var printPath by rememberSaveable { mutableStateOf<String?>(null) }
    val printFile = printPath?.let(::File)

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                preparing = true
                imagePaths = imagePaths + uris.mapNotNull { FileStore.copyDocumentToCache(context, it, "image") }.map { it.absolutePath }
                preparing = false
                error = null
            }
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                preparing = true
                pdfPaths = pdfPaths + uris.mapNotNull { FileStore.copyDocumentToCache(context, it, "pdf") }.map { it.absolutePath }
                preparing = false
                error = null
            }
        }
    }
    val singlePdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                preparing = true
                sourcePath = FileStore.copyDocumentToCache(context, uri, "source")?.absolutePath
                preparing = false
                outcome = null
                error = null
            }
        }
    }
    val printPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                preparing = true
                printPath = FileStore.copyDocumentToCache(context, uri, "print")?.absolutePath
                preparing = false
                outcome = null
                error = if (printPath == null) "Couldn't open that document." else null
            }
        }
    }

    fun clearOutcome() {
        outcome = null
        error = null
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "PDF Studio",
            onBack = onBack,
            eyebrow = "Make the document you need",
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = PagePadding),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(tools.size) { index ->
                    AppChip(
                        label = tools[index],
                        selected = tool == index,
                        onClick = {
                            tool = index
                            clearOutcome()
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            outcome?.let { result ->
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    accentTint = true,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Check, null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(9.dp))
                            BasicText(
                                text = result.title,
                                style = type.cardTitle.copy(color = colors.textPrimary),
                            )
                            Spacer(Modifier.weight(1f))
                            BasicText(
                                text = "Open library",
                                style = type.caption.copy(color = colors.accentInk, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.accentSoft)
                                    .then(Modifier.clickablePreview(onOpenLibrary))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                        BasicText(
                            text = result.detail,
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 6.dp, start = 39.dp),
                        )
                        if (result.pages.isNotEmpty()) {
                            Spacer(Modifier.height(7.dp))
                            SecondaryButton(
                                label = "Share first page",
                                onClick = {
                                    result.pages.firstOrNull()?.let {
                                        FileStore.share(context, it.absolutePath, "image/png")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(start = 39.dp),
                            )
                        }
                        val printable = result.file ?: result.pages.firstOrNull()
                        if (printable != null && Printer.isPrintable(printable)) {
                            Spacer(Modifier.height(7.dp))
                            SecondaryButton(
                                label = "Print document",
                                onClick = {
                                    Printer.print(context, printable, monochrome = prefs.printMonochrome)
                                        .onSuccess { error = null }
                                        .onFailure {
                                            error = it.message ?: "Couldn't open the print preview."
                                        }
                                },
                                icon = Icons.Outlined.Print,
                                modifier = Modifier.fillMaxWidth().padding(start = 39.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            when (tool) {
                0 -> {
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        BasicText(
                            text = "Write something worth keeping.",
                            style = type.serif.copy(color = colors.textPrimary, fontSize = 23.sp),
                        )
                        BasicText(
                            text = "Evexia typesets your text into a clean, paginated PDF with a proper title page.",
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        AppTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it; clearOutcome() },
                            label = "Document title",
                            placeholder = "e.g. Modern History — Chapter 4",
                            imeAction = ImeAction.Next,
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = body,
                            onValueChange = { body = it.take(60_000); clearOutcome() },
                            label = "Content",
                            placeholder = "Paste or write your notes here…",
                            singleLine = false,
                            minLines = 9,
                            maxLines = 20,
                            supporting = "${body.length}/60,000",
                            imeAction = ImeAction.Default,
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = name,
                            onValueChange = { name = it; clearOutcome() },
                            label = "File name (optional)",
                            placeholder = "history-chapter-4",
                            imeAction = ImeAction.Done,
                        )
                        Spacer(Modifier.height(16.dp))
                        CommonOptions(
                            subjects = state.subjects,
                            subjectId = subjectId,
                            onSubject = { subjectId = it },
                            pageSize = pageSize,
                            onPageSize = { pageSize = it },
                            filter = filter,
                            onFilter = { filter = it },
                            showFilter = false,
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            label = "Create PDF",
                            onClick = {
                                val title = docTitle.trim()
                                when {
                                    title.isBlank() -> error = "Add a document title."
                                    body.isBlank() -> error = "Write some content first."
                                    else -> {
                                        clearOutcome()
                                        onCreateTextPdf(
                                            title,
                                            body,
                                            fileName(name, title),
                                            PdfPageSize.from(pageSize),
                                            subjectId,
                                        ) { result ->
                                            result.onSuccess {
                                                outcome = ConversionOutcome(
                                                    "Text PDF created",
                                                    "${it.file.name} · ${it.pageCount} ${if (it.pageCount == 1) "page" else "pages"} · saved to Library",
                                                    it.file,
                                                )
                                            }.onFailure { error = it.message }
                                        }
                                    }
                                }
                            },
                            icon = Icons.Outlined.AutoStories,
                            enabled = !busy,
                            loading = busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                1 -> {
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        BasicText(
                            text = "Photos into one PDF.",
                            style = type.serif.copy(color = colors.textPrimary, fontSize = 23.sp),
                        )
                        BasicText(
                            text = "Choose pages in the order you want them. Each image becomes a full page.",
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            SecondaryButton(
                                label = if (images.isEmpty()) "Choose images" else "Add images",
                                onClick = { imagePicker.launch("image/*") },
                                icon = Icons.Outlined.PhotoLibrary,
                                modifier = Modifier.weight(1f),
                                enabled = !preparing,
                            )
                            if (images.isNotEmpty()) {
                                SecondaryButton(
                                    label = "Clear",
                                    onClick = { imagePaths = emptyList() },
                                    modifier = Modifier.weight(0.45f),
                                )
                            }
                        }
                        if (preparing) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spinner(size = 15.dp, color = colors.accentInk)
                                Spacer(Modifier.width(7.dp))
                                BasicText("Preparing images…", style = type.caption.copy(color = colors.textSecondary))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FilePickerList(
                            files = images,
                            emptyTitle = "No images yet",
                            emptyMessage = "Photos, screenshots or exported pages all work.",
                            icon = Icons.Outlined.Image,
                            onRemove = { imagePaths = imagePaths - it.absolutePath },
                        )
                        Spacer(Modifier.height(16.dp))
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "File name (optional)",
                            placeholder = "lecture-pages",
                            imeAction = ImeAction.Done,
                        )
                        Spacer(Modifier.height(16.dp))
                        CommonOptions(
                            subjects = state.subjects,
                            subjectId = subjectId,
                            onSubject = { subjectId = it },
                            pageSize = pageSize,
                            onPageSize = { pageSize = it },
                            filter = filter,
                            onFilter = { filter = it },
                            showFilter = true,
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            label = if (images.isEmpty()) "Choose images to begin" else "Create PDF · ${images.size} pages",
                            onClick = {
                                clearOutcome()
                                onCreateImagePdf(
                                    images,
                                    fileName(name, "Images ${DateUtils.shortDate(LocalDate.now())}"),
                                    PdfPageSize.from(pageSize),
                                    ScanFilter.from(filter),
                                    subjectId,
                                ) { result ->
                                    result.onSuccess {
                                        outcome = ConversionOutcome(
                                            "Image PDF created",
                                            "${it.file.name} · ${it.pageCount} ${if (it.pageCount == 1) "page" else "pages"} · saved to Library",
                                            it.file,
                                        )
                                    }.onFailure { error = it.message }
                                }
                            },
                            icon = Icons.Outlined.PictureAsPdf,
                            enabled = images.isNotEmpty() && !busy && !preparing,
                            loading = busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                2 -> {
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        BasicText(
                            text = "Many PDFs. One document.",
                            style = type.serif.copy(color = colors.textPrimary, fontSize = 23.sp),
                        )
                        BasicText(
                            text = "Choose two or more PDFs. They'll appear in the order you add them.",
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton(
                            label = if (pdfs.isEmpty()) "Choose PDFs" else "Add more PDFs",
                            onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                            icon = Icons.Filled.Add,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !preparing,
                        )
                        if (preparing) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spinner(size = 15.dp, color = colors.accentInk)
                                Spacer(Modifier.width(7.dp))
                                BasicText("Reading PDFs…", style = type.caption.copy(color = colors.textSecondary))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FilePickerList(
                            files = pdfs,
                            emptyTitle = "No PDFs chosen",
                            emptyMessage = "Add at least two documents to merge.",
                            icon = Icons.Outlined.PictureAsPdf,
                            onRemove = { pdfPaths = pdfPaths - it.absolutePath },
                        )
                        Spacer(Modifier.height(16.dp))
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "File name (optional)",
                            placeholder = "combined-notes",
                            imeAction = ImeAction.Done,
                        )
                        Spacer(Modifier.height(14.dp))
                        SubjectPicker(state.subjects, subjectId) { subjectId = it }
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            label = if (pdfs.size < 2) "Add at least two PDFs" else "Merge ${pdfs.size} PDFs",
                            onClick = {
                                clearOutcome()
                                onMergePdfs(
                                    pdfs,
                                    fileName(name, "Merged ${DateUtils.shortDate(LocalDate.now())}"),
                                    subjectId,
                                ) { result ->
                                    result.onSuccess {
                                        outcome = ConversionOutcome(
                                            "PDFs merged",
                                            "${it.file.name} · ${it.pageCount} pages · saved to Library",
                                            it.file,
                                        )
                                    }.onFailure { error = it.message }
                                }
                            },
                            icon = Icons.Outlined.MergeType,
                            enabled = pdfs.size >= 2 && !busy && !preparing,
                            loading = busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                3 -> {
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        BasicText(
                            text = "Print what matters.",
                            style = type.serif.copy(color = colors.textPrimary, fontSize = 23.sp),
                        )
                        BasicText(
                            text = "Choose a PDF or image and send it to any printer installed on this device — or save it as a PDF from Android's print screen.",
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton(
                            label = if (printFile == null) "Choose a document" else "Choose another document",
                            onClick = { printPicker.launch(arrayOf("application/pdf", "image/*")) },
                            icon = Icons.Outlined.Print,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !preparing,
                        )
                        if (preparing) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spinner(size = 15.dp, color = colors.accentInk)
                                Spacer(Modifier.width(7.dp))
                                BasicText("Preparing the document…", style = type.caption.copy(color = colors.textSecondary))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FilePickerList(
                            files = printFile?.let(::listOf) ?: emptyList(),
                            emptyTitle = "No document chosen",
                            emptyMessage = "PDFs and images can be printed directly from Evexia.",
                            icon = Icons.Outlined.Print,
                            onRemove = { printPath = null },
                        )
                        Spacer(Modifier.height(16.dp))
                        BasicText(
                            "Print colour",
                            style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        SegmentedControl(
                            items = listOf("Colour", "Black & white"),
                            selectedIndex = if (prefs.printMonochrome) 1 else 0,
                            onSelected = { onSetPrintMonochrome(it == 1) },
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            label = "Open print preview",
                            onClick = {
                                printFile?.let { file ->
                                    Printer.print(context, file, monochrome = prefs.printMonochrome)
                                        .onSuccess { error = null }
                                        .onFailure {
                                            error = it.message ?: "Couldn't open the print preview."
                                        }
                                }
                            },
                            icon = Icons.Outlined.Print,
                            enabled = printFile != null && !preparing,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        BasicText(
                            text = "Android shows nearby printers and its own Save as PDF option. This colour choice is remembered for all Evexia printouts. Evexia never uploads the document.",
                            style = type.caption.copy(color = colors.textTertiary),
                        )
                    }
                }
                else -> {
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        BasicText(
                            text = "Give every page a second life.",
                            style = type.serif.copy(color = colors.textPrimary, fontSize = 23.sp),
                        )
                        BasicText(
                            text = "Turn each PDF page into a crisp PNG image — useful for slides, posters or sharing.",
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton(
                            label = if (sourcePdf == null) "Choose a PDF" else "Choose another PDF",
                            onClick = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                            icon = Icons.Outlined.Description,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !preparing,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (sourcePdf != null) {
                            GlassSurface(
                                Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.PictureAsPdf,
                                        null,
                                        tint = colors.accentInk,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        BasicText(
                                            text = sourcePdf!!.name,
                                            style = type.cardTitle.copy(color = colors.textPrimary),
                                            maxLines = 1,
                                        )
                                        BasicText(
                                            text = FileStore.humanSize(sourcePdf!!.length()),
                                            style = type.caption.copy(color = colors.textSecondary),
                                        )
                                    }
                                    IconButton(
                                        icon = Icons.Filled.Close,
                                        contentDescription = "Remove PDF",
                                        onClick = { sourcePath = null },
                                        size = 32.dp,
                                        glass = false,
                                    )
                                }
                            }
                        } else {
                            FilePickerList(
                                files = emptyList(),
                                emptyTitle = "No PDF chosen",
                                emptyMessage = "Pick one document to export as images.",
                                icon = Icons.Outlined.PictureAsPdf,
                                onRemove = {},
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Image prefix (optional)",
                            placeholder = "lecture-slides",
                            imeAction = ImeAction.Done,
                        )
                        Spacer(Modifier.height(14.dp))
                        BasicText(
                            text = "Images are saved inside Evexia's exports folder, on this device.",
                            style = type.caption.copy(color = colors.textTertiary),
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            label = if (sourcePdf == null) "Choose a PDF to begin" else "Export pages as PNG",
                            onClick = {
                                clearOutcome()
                                val file = sourcePdf
                                if (file != null) onPdfToImages(
                                    file,
                                    fileName(name, file.nameWithoutExtension),
                                ) { result ->
                                    result.onSuccess { pages ->
                                        outcome = ConversionOutcome(
                                            "Pages exported",
                                            "${pages.size} ${if (pages.size == 1) "image" else "images"} ready to share",
                                            pages = pages,
                                        )
                                    }.onFailure { error = it.message }
                                }
                            },
                            icon = Icons.Outlined.Image,
                            enabled = sourcePdf != null && !busy && !preparing,
                            loading = busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                BasicText(
                    text = it,
                    style = type.caption.copy(color = colors.danger, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                BasicText(
                    text = "Everything is processed on this device. Nothing is uploaded.",
                    style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun CommonOptions(
    subjects: List<SubjectEntity>,
    subjectId: Long?,
    onSubject: (Long?) -> Unit,
    pageSize: String,
    onPageSize: (String) -> Unit,
    filter: String,
    onFilter: (String) -> Unit,
    showFilter: Boolean,
) {
    val colors = DF.colors
    val type = DF.type
    BasicText(
        "Page size",
        style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
    SegmentedControl(
        items = PdfPageSize.entries.map { it.label },
        selectedIndex = PdfPageSize.entries.indexOf(PdfPageSize.from(pageSize)),
        onSelected = { onPageSize(PdfPageSize.entries[it].name) },
    )
    if (showFilter) {
        Spacer(Modifier.height(14.dp))
        BasicText(
            "Finish",
            style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            items = ScanFilter.entries.map { it.label },
            selectedIndex = ScanFilter.entries.indexOf(ScanFilter.from(filter)),
            onSelected = { onFilter(ScanFilter.entries[it].name) },
        )
    }
    Spacer(Modifier.height(14.dp))
    SubjectPicker(subjects, subjectId, onSubject)
}

@Composable
private fun SubjectPicker(
    subjects: List<SubjectEntity>,
    subjectId: Long?,
    onSubject: (Long?) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    BasicText(
        "Subject",
        style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        item { AppChip("No subject", subjectId == null, onClick = { onSubject(null) }) }
        items(subjects, key = { it.id }) { subject ->
            AppChip(
                label = subject.name,
                selected = subjectId == subject.id,
                onClick = { onSubject(subject.id) },
                dotColor = subjectColor(subject),
            )
        }
    }
}

@Composable
private fun FilePickerList(
    files: List<File>,
    emptyTitle: String,
    emptyMessage: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRemove: (File) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    if (files.isEmpty()) {
        GlassSurface(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            elevated = false,
        ) {
            EmptyState(
                icon = icon,
                title = emptyTitle,
                message = emptyMessage,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    } else {
        GlassSurface(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            Column {
                files.forEachIndexed { index, file ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.glassQuiet),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "${index + 1}",
                                style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                text = file.name,
                                style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.sp),
                                maxLines = 1,
                            )
                            BasicText(
                                text = FileStore.humanSize(file.length()),
                                style = type.caption.copy(color = colors.textTertiary),
                            )
                        }
                        IconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = "Remove",
                            onClick = { onRemove(file) },
                            size = 32.dp,
                            glass = false,
                        )
                    }
                }
            }
        }
    }
}

private fun fileName(raw: String, fallback: String): String {
    val base = raw.trim().ifBlank { fallback }
    return base.removeSuffix(".pdf").removeSuffix(".PDF").ifBlank { fallback }
}

private fun Modifier.clickablePreview(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

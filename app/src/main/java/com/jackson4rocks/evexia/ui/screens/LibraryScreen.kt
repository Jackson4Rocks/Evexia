package com.jackson4rocks.evexia.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Print
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AccentIconButton
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.GlassSheet
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.LibraryRow
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SegmentedControl
import com.jackson4rocks.evexia.ui.components.subjectColor
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.FileStore
import com.jackson4rocks.evexia.util.Printer
import java.io.File

@Composable
fun LibraryScreen(
    state: AppUiState,
    initialTab: Int,
    initialSubjectId: Long?,
    onBack: () -> Unit,
    onImportFiles: (List<Uri>, Long?) -> Unit,
    onAddBookmark: (String, String, Long?) -> Unit,
    onOpenItem: (LibraryItemEntity) -> Unit,
    onShareItem: (LibraryItemEntity) -> Unit,
    onPrintItem: (LibraryItemEntity) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onMoveItem: (Long, Long?) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var subjectFilter by rememberSaveable { mutableStateOf(initialSubjectId) }
    var showLinkSheet by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<LibraryItemEntity?>(null) }
    var moving by remember { mutableStateOf<LibraryItemEntity?>(null) }
    var deleting by remember { mutableStateOf<LibraryItemEntity?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        onImportFiles(uris, subjectFilter)
    }
    val files = state.library.filter { it.kind != LibraryItemEntity.KIND_BOOKMARK }
    val links = state.library.filter { it.kind == LibraryItemEntity.KIND_BOOKMARK }
    val filtered = state.library
        .filter { subjectFilter == null || it.subjectId == subjectFilter }
        .let {
            when (tab) {
                1 -> it.filter { item -> item.lastOpenedAt != null }
                    .sortedByDescending { item -> item.lastOpenedAt ?: 0L }
                2 -> it.filter { item -> item.kind == LibraryItemEntity.KIND_BOOKMARK }
                else -> it.sortedByDescending { item -> item.addedAt }
            }
        }
    val totalSize = files.sumOf { it.sizeBytes }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Library",
            onBack = onBack,
            eyebrow = "Files & bookmarks",
            actions = {
                IconButton(
                    icon = Icons.Filled.Link,
                    contentDescription = "Save a link",
                    onClick = { showLinkSheet = true },
                    size = 40.dp,
                )
                Spacer(Modifier.width(7.dp))
                AccentIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Import files",
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    size = 40.dp,
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            accentTint = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.FolderOpen, null, tint = colors.accentInk, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "${files.size} ${if (files.size == 1) "file" else "files"} · ${links.size} ${if (links.size == 1) "link" else "links"}",
                        style = type.cardTitle.copy(color = colors.textPrimary),
                    )
                    BasicText(
                        text = "${FileStore.humanSize(totalSize)} stored on this device",
                        style = type.caption.copy(color = colors.textSecondary),
                    )
                }
                BasicText(
                    text = "LOCAL",
                    style = type.label.copy(color = colors.accentInk, fontSize = 8.sp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.padding(horizontal = PagePadding)) {
            SegmentedControl(
                items = listOf("All", "Recent", "Links"),
                selectedIndex = tab,
                onSelected = { tab = it },
            )
        }
        Spacer(Modifier.height(11.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item {
                AppChip(
                    label = "All subjects",
                    selected = subjectFilter == null,
                    onClick = { subjectFilter = null },
                )
            }
            items(state.subjects, key = { it.id }) { subject ->
                AppChip(
                    label = subject.name,
                    selected = subjectFilter == subject.id,
                    onClick = { subjectFilter = if (subjectFilter == subject.id) null else subject.id },
                    dotColor = subjectColor(subject),
                )
            }
        }
        Spacer(Modifier.height(13.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = BottomContentPadding),
        ) {
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = if (tab == 2) Icons.Filled.Link else Icons.Outlined.FolderOpen,
                    title = when (tab) {
                        1 -> "No recent files"
                        2 -> "No saved links"
                        else -> "Your library is empty"
                    },
                    message = when (tab) {
                        2 -> "Keep a useful site or reading close to the work it belongs to."
                        1 -> "Open a file and it will appear here for a quick return."
                        else -> "Import lecture notes, PDFs and images. Everything stays on this device."
                    },
                    actionLabel = if (tab == 2) "Save a link" else "Import files",
                    onAction = {
                        if (tab == 2) showLinkSheet = true else importLauncher.launch(arrayOf("*/*"))
                    },
                )
            } else {
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(vertical = 3.dp),
                ) {
                    Column {
                        filtered.forEachIndexed { index, item ->
                            LibraryRow(
                                item = item,
                                subject = state.subject(item.subjectId),
                                onClick = { onOpenItem(item) },
                                onMore = { selected = item },
                            )
                            if (index < filtered.lastIndex) DividerLine()
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BasicText(
                        text = "Tap to open · more for print, move, share or delete",
                        style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                    )
                }
            }
        }
    }

    if (showLinkSheet) {
        AddLinkSheet(
            subjects = state.subjects,
            initialSubject = subjectFilter,
            onDismiss = { showLinkSheet = false },
            onSave = { title, url, subject ->
                showLinkSheet = false
                onAddBookmark(title, url, subject)
            },
        )
    }

    selected?.let { item ->
        GlassSheet(
            title = item.title,
            subtitle = when {
                item.kind == LibraryItemEntity.KIND_BOOKMARK -> "Saved link"
                item.subjectId != null -> "In ${state.subjectName(item.subjectId)}"
                else -> "In your library"
            },
            onDismiss = { selected = null },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PrimaryButton(
                    label = if (item.kind == LibraryItemEntity.KIND_BOOKMARK) "Open link" else "Open file",
                    onClick = {
                        selected = null
                        onOpenItem(item)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (item.kind == LibraryItemEntity.KIND_FILE) {
                    if (Printer.isPrintable(File(item.location))) {
                        SecondaryButton(
                            label = "Print",
                            onClick = {
                                selected = null
                                onPrintItem(item)
                            },
                            icon = Icons.Outlined.Print,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SecondaryButton(
                        label = "Share a copy",
                        onClick = {
                            selected = null
                            onShareItem(item)
                        },
                        icon = Icons.Filled.Share,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SecondaryButton(
                    label = "Move to subject",
                    onClick = {
                        moving = item
                        selected = null
                    },
                    icon = Icons.Outlined.FolderOpen,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    label = "Delete",
                    onClick = {
                        deleting = item
                        selected = null
                    },
                    icon = Icons.Filled.DeleteOutline,
                    modifier = Modifier.fillMaxWidth(),
                    danger = true,
                )
            }
        }
    }

    moving?.let { item ->
        GlassSheet(
            title = "Move to subject",
            subtitle = "Keep related work together. The file itself stays where it is.",
            onDismiss = { moving = null },
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    AppChip(
                        label = "No subject",
                        selected = item.subjectId == null,
                        onClick = {
                            onMoveItem(item.id, null)
                            moving = null
                        },
                    )
                }
                items(state.subjects, key = { it.id }) { subject ->
                    AppChip(
                        label = subject.name,
                        selected = item.subjectId == subject.id,
                        onClick = {
                            onMoveItem(item.id, subject.id)
                            moving = null
                        },
                        dotColor = subjectColor(subject),
                    )
                }
            }
        }
    }

    deleting?.let { item ->
        ConfirmDialog(
            title = "Remove from library?",
            message = if (item.kind == LibraryItemEntity.KIND_FILE) {
                "\"${item.title}\" and its stored copy will be deleted. This can't be undone."
            } else "\"${item.title}\" will be removed from your saved links.",
            confirmLabel = "Remove",
            onConfirm = {
                deleting = null
                onDeleteItem(item.id)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun AddLinkSheet(
    subjects: List<com.jackson4rocks.evexia.data.SubjectEntity>,
    initialSubject: Long?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    var title by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var subjectId by rememberSaveable { mutableStateOf(initialSubject) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassSheet(
        title = "Save a link",
        subtitle = "A reading, a resource, or something to come back to.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = url,
            onValueChange = { url = it.trim(); error = null },
            label = "Web address",
            placeholder = "https://example.com/article",
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next,
            leadingIcon = Icons.Filled.Link,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = title,
            onValueChange = { if (it.length <= 100) title = it },
            label = "Title (optional)",
            placeholder = "What is this?",
            supporting = "We'll use the site name if you leave this blank",
            imeAction = ImeAction.Done,
            error = error,
        )
        Spacer(Modifier.height(16.dp))
        BasicText(
            "Subject",
            style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { AppChip("No subject", subjectId == null, onClick = { subjectId = null }) }
            items(subjects, key = { it.id }) { subject ->
                AppChip(
                    label = subject.name,
                    selected = subjectId == subject.id,
                    onClick = { subjectId = subject.id },
                    dotColor = subjectColor(subject),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            label = "Save link",
            onClick = {
                val clean = url.trim()
                if (clean.isBlank()) {
                    error = null
                    // handled below so the field can show the message
                }
                if (clean.isBlank()) {
                    error = "Add a web address."
                } else {
                    val normalised = if (clean.startsWith("http://") || clean.startsWith("https://")) {
                        clean
                    } else "https://$clean"
                    if (!normalised.contains('.') || normalised.endsWith('.')) {
                        error = "That doesn't look like a web address."
                    } else {
                        onSave(title.trim(), normalised, subjectId)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = "Links open in your browser. Evexia keeps only the address.",
            style = type.caption.copy(color = colors.textTertiary),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

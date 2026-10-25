package com.jackson4rocks.evexia.ui.screens

import android.net.Uri
import android.util.Patterns
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.data.TaskEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AccentIconButton
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.BackButton
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.CompactEmpty
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.FileGlyph
import com.jackson4rocks.evexia.ui.components.FlashcardSetRow
import com.jackson4rocks.evexia.ui.components.GlassSheet
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.InfoRow
import com.jackson4rocks.evexia.ui.components.LibraryRow
import com.jackson4rocks.evexia.ui.components.NoteRow
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.Radii
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.ScreenTitle
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SectionHeader
import com.jackson4rocks.evexia.ui.components.SegmentedControl
import com.jackson4rocks.evexia.ui.components.SubjectCard
import com.jackson4rocks.evexia.ui.components.TaskRow
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.components.readableInk
import com.jackson4rocks.evexia.ui.components.subjectColor
import com.jackson4rocks.evexia.ui.components.subjectIcon
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.SubjectPalette
import com.jackson4rocks.evexia.util.FileStore
import java.io.File
import java.net.URI

private val iconChoices = listOf(
    "book" to Icons.Outlined.MenuBook,
    "science" to Icons.Outlined.Science,
    "math" to Icons.Outlined.Calculate,
    "language" to Icons.Outlined.Translate,
    "arts" to Icons.Outlined.Palette,
    "history" to Icons.Outlined.AccountBalance,
    "computer" to Icons.Outlined.Computer,
    "music" to Icons.Outlined.MusicNote,
)

@Composable
fun SubjectsScreen(
    state: AppUiState,
    onSubject: (Long) -> Unit,
    onAddSubject: () -> Unit,
    onLibrary: () -> Unit,
    onStudy: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val openTasks = state.tasks.count { !it.completed }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "Subjects",
            eyebrow = "Your library",
            subtitle = "${state.subjects.size} ${if (state.subjects.size == 1) "course" else "courses"} · $openTasks open tasks",
            trailing = {
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(colors.glassQuiet)
                        .border(1.dp, colors.border, CircleShape)
                        .pressable(onClick = onLibrary)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        text = "Library",
                        style = type.caption.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                AccentIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add subject",
                    onClick = onAddSubject,
                    size = 40.dp,
                )
            },
        )
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PagePadding, end = PagePadding, bottom = BottomContentPadding + 12.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StudyHubCard(
                    notes = state.notes.size,
                    sets = state.flashcardSets.size,
                    cards = state.flashcards.size,
                    onClick = onStudy,
                )
            }
            if (state.subjects.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = "A place for every course",
                        message = "Create a subject folder to group tasks, notes, files and the links you keep coming back to.",
                        actionLabel = "Create a subject",
                        onAction = onAddSubject,
                    )
                }
            } else {
                gridItems(state.subjects, key = { it.id }) { subject ->
                    SubjectCard(
                        subject = subject,
                        openTasks = state.tasks.count { it.subjectId == subject.id && !it.completed },
                        resources = state.library.count { it.subjectId == subject.id } +
                            state.notes.count { it.subjectId == subject.id } +
                            state.flashcardSets.count { it.subjectId == subject.id },
                        onClick = { onSubject(subject.id) },
                    )
                }
                item {
                    AddSubjectTile(onClick = onAddSubject)
                }
            }
        }
    }
}

@Composable
private fun StudyHubCard(
    notes: Int,
    sets: Int,
    cards: Int,
    onClick: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.hero,
        accentTint = true,
        contentPadding = PaddingValues(18.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = "Study",
                    style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 16.sp),
                )
                BasicText(
                    text = "$notes ${if (notes == 1) "note" else "notes"} · " +
                        "$sets ${if (sets == 1) "set" else "sets"} · " +
                        "$cards ${if (cards == 1) "card" else "cards"}",
                    style = type.caption.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.glassQuiet)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "Open",
                    style = type.caption.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun AddSubjectTile(onClick: () -> Unit) {
    val colors = DF.colors
    val type = DF.type
    Box(
        Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.glassQuiet),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            BasicText(
                text = "New subject",
                style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
fun SubjectDetailScreen(
    subjectId: Long,
    state: AppUiState,
    onBack: () -> Unit,
    onEditSubject: (Long) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onAddTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onAddNote: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onAddSet: (Long) -> Unit,
    onOpenSet: (Long) -> Unit,
    onImportFiles: (Long, List<Uri>) -> Unit,
    onAddLink: (Long, String, String) -> Unit,
    onOpenLibraryItem: (LibraryItemEntity) -> Unit,
    onDeleteLibraryItem: (Long) -> Unit,
    onLibrary: () -> Unit,
    onStudy: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val subject = state.subjects.firstOrNull { it.id == subjectId }
    if (subject == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "Folder not found",
                message = "This subject may have been deleted.",
                actionLabel = "Go back",
                onAction = onBack,
            )
        }
        return
    }

    val tasks = state.tasks.filter { it.subjectId == subjectId }
    val notes = state.notes.filter { it.subjectId == subjectId }
    val sets = state.flashcardSets.filter { it.subjectId == subjectId }
    val library = state.library.filter { it.subjectId == subjectId }
    val accent = subjectColor(subject)

    var tab by rememberSaveable(subjectId) { mutableStateOf(0) }
    var taskFilter by rememberSaveable(subjectId) { mutableStateOf(0) }
    var showDelete by remember { mutableStateOf(false) }
    var libraryActionsFor by remember { mutableStateOf<LibraryItemEntity?>(null) }
    var deleteItemFor by remember { mutableStateOf<LibraryItemEntity?>(null) }
    var showLinkSheet by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) onImportFiles(subjectId, uris)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = BottomContentPadding + 16.dp),
    ) {
        ScreenHeader(
            title = subject.name,
            onBack = onBack,
            eyebrow = "Subject folder",
            actions = {
                IconButton(
                    icon = Icons.Filled.MoreHoriz,
                    contentDescription = "Subject options",
                    onClick = { onEditSubject(subjectId) },
                    size = 40.dp,
                )
            },
        )

        GlassSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            shape = Radii.hero,
            tint = accent.copy(alpha = if (colors.dark) 0.17f else 0.26f),
            contentPadding = PaddingValues(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            subjectIcon(subject.icon),
                            null,
                            tint = accent.readableInk(),
                            modifier = Modifier.size(29.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = subject.name,
                            style = type.sectionTitle.copy(color = colors.textPrimary, fontSize = 21.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            text = "${tasks.count { !it.completed }} open tasks · ${notes.size} notes · ${library.size} materials",
                            style = type.caption.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        label = "Add task",
                        onClick = { onAddTask(subjectId) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Add,
                    )
                    SecondaryButton(
                        label = "Add note",
                        onClick = { onAddNote(subjectId) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.NoteAlt,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SegmentedControl(
            items = listOf("Overview", "Tasks", "Notes", "Files"),
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(horizontal = PagePadding),
        )
        Spacer(Modifier.height(18.dp))

        when (tab) {
            0 -> SubjectOverview(
                tasks = tasks,
                notesCount = notes.size,
                setsCount = sets.size,
                filesCount = library.size,
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                onOpenNote = onOpenNote,
                onOpenSet = onOpenSet,
                sets = sets,
                notes = notes,
                onAddTask = { onAddTask(subjectId) },
                onSeeTasks = { tab = 1 },
            )

            1 -> {
                val visible = if (taskFilter == 0) tasks.filter { !it.completed } else tasks.filter { it.completed }
                SectionHeader(title = "Tasks", action = "Add", onAction = { onAddTask(subjectId) })
                Spacer(Modifier.height(9.dp))
                SegmentedControl(
                    items = listOf("Open", "Completed"),
                    selectedIndex = taskFilter,
                    onSelected = { taskFilter = it },
                    modifier = Modifier.padding(horizontal = PagePadding),
                )
                Spacer(Modifier.height(10.dp))
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(vertical = 3.dp),
                ) {
                    Column {
                        if (visible.isEmpty()) {
                            CompactEmpty(
                                title = if (taskFilter == 0) "No open tasks" else "Nothing completed yet",
                                message = if (taskFilter == 0) "Keep this folder light until something is due." else "Finished work will collect here.",
                                action = "Add task",
                                onAction = { onAddTask(subjectId) },
                            )
                        } else {
                            visible.forEachIndexed { index, task ->
                                TaskRow(
                                    task = task,
                                    subject = subject,
                                    onToggle = { onToggleTask(task) },
                                    onClick = { onEditTask(task.id) },
                                )
                                if (index < visible.lastIndex) DividerLine()
                            }
                        }
                    }
                }
            }

            2 -> {
                SectionHeader(title = "Notes", action = "Add", onAction = { onAddNote(subjectId) })
                Spacer(Modifier.height(10.dp))
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(vertical = 3.dp),
                ) {
                    Column {
                        if (notes.isEmpty()) {
                            CompactEmpty(
                                title = "No notes here yet",
                                message = "Write down the parts you don't want to lose.",
                                action = "Write",
                                onAction = { onAddNote(subjectId) },
                            )
                        } else {
                            notes.forEachIndexed { index, note ->
                                NoteRow(note = note, subject = subject, onClick = { onOpenNote(note.id) })
                                if (index < notes.lastIndex) DividerLine()
                            }
                        }
                    }
                }
                if (sets.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    SectionHeader(title = "Study sets", action = "All", onAction = onStudy)
                    Spacer(Modifier.height(10.dp))
                    GlassSurface(
                        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                        contentPadding = PaddingValues(vertical = 3.dp),
                    ) {
                        Column {
                            sets.forEachIndexed { index, set ->
                                FlashcardSetRow(
                                    set = set,
                                    subject = subject,
                                    cardCount = state.cardsFor(set.id).size,
                                    onClick = { onOpenSet(set.id) },
                                )
                                if (index < sets.lastIndex) DividerLine()
                            }
                        }
                    }
                }
            }

            else -> {
                SectionHeader(title = "Files & links", action = "All files", onAction = onLibrary)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.padding(horizontal = PagePadding),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    SecondaryButton(
                        label = "Import file",
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        label = "Save link",
                        onClick = { showLinkSheet = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(vertical = 3.dp),
                ) {
                    Column {
                        if (library.isEmpty()) {
                            CompactEmpty(
                                title = "No materials saved",
                                message = "Bring in a PDF or keep a useful link close to the course.",
                                action = "Import",
                                onAction = { filePicker.launch(arrayOf("*/*")) },
                            )
                        } else {
                            library.forEachIndexed { index, item ->
                                LibraryRow(
                                    item = item,
                                    subject = subject,
                                    onClick = { onOpenLibraryItem(item) },
                                    onMore = { libraryActionsFor = item },
                                )
                                if (index < library.lastIndex) DividerLine()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this subject?",
            message = "Its tasks, notes and files stay in Evexia but will no longer belong to a folder. This can't be undone.",
            confirmLabel = "Delete subject",
            onConfirm = {
                showDelete = false
                onDeleteSubject(subjectId)
            },
            onDismiss = { showDelete = false },
        )
    }

    if (libraryActionsFor != null) {
        val item = libraryActionsFor!!
        val isLink = item.kind == LibraryItemEntity.KIND_BOOKMARK
        GlassSheet(
            title = item.title,
            subtitle = if (isLink) "Saved link" else FileStore.humanSize(item.sizeBytes),
            onDismiss = { libraryActionsFor = null },
        ) {
            InfoRow(
                icon = if (isLink) Icons.Outlined.Language else Icons.Outlined.FolderOpen,
                title = if (isLink) "Open link" else "Open document",
                onClick = {
                    libraryActionsFor = null
                    onOpenLibraryItem(item)
                },
            )
            if (!isLink) {
                InfoRow(
                    icon = Icons.Filled.Share,
                    title = "Share a copy",
                    onClick = {
                        FileStore.share(context, item.location, item.mimeType)
                        libraryActionsFor = null
                    },
                )
            }
            InfoRow(
                icon = Icons.Filled.DeleteOutline,
                title = "Remove from library",
                iconTint = colors.danger,
                onClick = {
                    libraryActionsFor = null
                    deleteItemFor = item
                },
            )
        }
    }

    deleteItemFor?.let { item ->
        ConfirmDialog(
            title = "Remove this item?",
            message = if (item.kind == LibraryItemEntity.KIND_BOOKMARK) {
                "The saved link will be removed from your library."
            } else {
                "The imported copy inside Evexia will be deleted. Your original file is untouched."
            },
            confirmLabel = "Remove",
            onConfirm = {
                onDeleteLibraryItem(item.id)
                deleteItemFor = null
            },
            onDismiss = { deleteItemFor = null },
        )
    }

    if (showLinkSheet) {
        AddLinkSheet(
            accent = accent,
            onDismiss = { showLinkSheet = false },
            onSave = { title, url ->
                onAddLink(subjectId, title, url)
                showLinkSheet = false
            },
        )
    }
}

@Composable
private fun SubjectOverview(
    tasks: List<TaskEntity>,
    notesCount: Int,
    setsCount: Int,
    filesCount: Int,
    sets: List<com.jackson4rocks.evexia.data.FlashcardSetEntity>,
    notes: List<com.jackson4rocks.evexia.data.NoteEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenSet: (Long) -> Unit,
    onAddTask: () -> Unit,
    onSeeTasks: () -> Unit,
) {
    val type = DF.type
    val colors = DF.colors
    val open = tasks.filter { !it.completed }.take(3)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        MetricTile("Open", open.size.toString(), Modifier.weight(1f))
        MetricTile("Notes", notesCount.toString(), Modifier.weight(1f))
        MetricTile("Saved", (filesCount + setsCount).toString(), Modifier.weight(1f))
    }
    Spacer(Modifier.height(22.dp))
    SectionHeader(title = "Next up", action = "See all", onAction = onSeeTasks)
    Spacer(Modifier.height(9.dp))
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
        contentPadding = PaddingValues(vertical = 3.dp),
    ) {
        Column {
            if (open.isEmpty()) {
                CompactEmpty(
                    title = "No open tasks",
                    message = "Nothing needs your attention in this subject.",
                    action = "Add",
                    onAction = onAddTask,
                )
            } else {
                open.forEachIndexed { index, task ->
                    TaskRow(
                        task = task,
                        subject = null,
                        onToggle = { onToggleTask(task) },
                        onClick = { onEditTask(task.id) },
                    )
                    if (index < open.lastIndex) DividerLine()
                }
            }
        }
    }
    if (notes.isNotEmpty()) {
        Spacer(Modifier.height(22.dp))
        SectionHeader(title = "Recent notes")
        Spacer(Modifier.height(9.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            contentPadding = PaddingValues(vertical = 3.dp),
        ) {
            Column {
                notes.take(2).forEachIndexed { index, note ->
                    NoteRow(note = note, subject = null, onClick = { onOpenNote(note.id) })
                    if (index < minOf(notes.size, 2) - 1) DividerLine()
                }
            }
        }
    }
    if (sets.isNotEmpty()) {
        Spacer(Modifier.height(22.dp))
        SectionHeader(title = "Study sets")
        Spacer(Modifier.height(9.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            contentPadding = PaddingValues(vertical = 3.dp),
        ) {
            Column {
                sets.take(2).forEachIndexed { index, set ->
                    FlashcardSetRow(
                        set = set,
                        subject = null,
                        cardCount = 0,
                        onClick = { onOpenSet(set.id) },
                    )
                    if (index < minOf(sets.size, 2) - 1) DividerLine()
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = DF.colors
    val type = DF.type
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        elevated = false,
    ) {
        Column {
            BasicText(text = value, style = type.serif.copy(color = colors.textPrimary, fontSize = 24.sp))
            BasicText(text = label, style = type.caption.copy(color = colors.textSecondary))
        }
    }
}

@Composable
fun SubjectEditorScreen(
    subjectId: Long?,
    state: AppUiState,
    onBack: () -> Unit,
    onSave: (SubjectEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val existing = subjectId?.let { state.subjects.firstOrNull { item -> item.id == it } }
    var name by rememberSaveable(subjectId) { mutableStateOf(existing?.name.orEmpty()) }
    var color by rememberSaveable(subjectId) { mutableStateOf(existing?.color ?: SubjectPalette.first().value.toLong()) }
    var icon by rememberSaveable(subjectId) { mutableStateOf(existing?.icon ?: "book") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (existing == null) "New subject" else "Edit subject",
            onBack = onBack,
            eyebrow = "Course folder",
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PagePadding),
        ) {
            GlassSurface(
                Modifier.fillMaxWidth(),
                shape = Radii.hero,
                tint = Color(color).copy(alpha = 0.24f),
                contentPadding = PaddingValues(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(color)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(subjectIcon(icon), null, tint = Color(color).readableInk(), modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        BasicText(
                            text = name.ifBlank { "Subject name" },
                            style = type.sectionTitle.copy(color = colors.textPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            text = "A folder for everything this course needs",
                            style = type.caption.copy(color = colors.textSecondary),
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            AppTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 50) name = it
                    error = null
                },
                label = "Subject name",
                placeholder = "e.g. Linear Algebra",
                error = error,
                supporting = "${name.length}/50",
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                onImeAction = {},
            )
            Spacer(Modifier.height(20.dp))
            TextLabel("Colour")
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(SubjectPalette) { swatch ->
                    val selected = color == swatch.value.toLong()
                    Box(
                        Modifier
                            .size(37.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) colors.textPrimary else colors.border,
                                shape = CircleShape,
                            )
                            .pressable { color = swatch.value.toLong() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(Icons.Filled.Check, null, tint = swatch.readableInk(), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            TextLabel("Symbol")
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(iconChoices) { (key, vector) ->
                    val selected = icon == key
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (selected) Color(color).copy(alpha = 0.28f) else colors.glassQuiet)
                            .border(
                                1.dp,
                                if (selected) Color(color) else colors.border,
                                RoundedCornerShape(15.dp),
                            )
                            .pressable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            vector,
                            null,
                            tint = if (selected) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
            if (existing != null) {
                Spacer(Modifier.height(30.dp))
                SecondaryButton(
                    label = "Delete subject",
                    onClick = { confirmDelete = true },
                    icon = Icons.Filled.DeleteOutline,
                    danger = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.background.copy(alpha = 0.94f))
                .padding(horizontal = PagePadding, vertical = 12.dp)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                label = if (existing == null) "Create subject" else "Save changes",
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        error = "Give this folder a name."
                        return@PrimaryButton
                    }
                    onSave(
                        SubjectEntity(
                            id = existing?.id ?: 0L,
                            name = trimmed,
                            color = color,
                            icon = icon,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete ${existing.name}?",
            message = "The folder will be removed. Anything inside stays in Evexia but becomes unassigned.",
            confirmLabel = "Delete folder",
            onConfirm = {
                confirmDelete = false
                onDelete(existing.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TextLabel(text: String) {
    BasicText(
        text = text,
        style = DF.type.caption.copy(color = DF.colors.textSecondary, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun AddLinkSheet(
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    GlassSheet(
        title = "Save a link",
        subtitle = "A lecture, a drive folder, a useful read.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = url,
            onValueChange = { url = it; error = null },
            label = "Web address",
            placeholder = "https://…",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = "Label (optional)",
            placeholder = "e.g. Lecture 4 slides",
        )
        error?.let {
            BasicText(
                text = it,
                style = DF.type.caption.copy(color = DF.colors.danger),
                modifier = Modifier.padding(start = 4.dp, top = 7.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            label = "Save link",
            onClick = {
                val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                if (url.isBlank() || !Patterns.WEB_URL.matcher(normalized).matches()) {
                    error = "That doesn't look like a web address."
                } else {
                    onSave(title, normalized)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

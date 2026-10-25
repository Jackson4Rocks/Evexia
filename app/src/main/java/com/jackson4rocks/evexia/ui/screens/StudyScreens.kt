package com.jackson4rocks.evexia.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.School
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.FlashcardEntity
import com.jackson4rocks.evexia.data.FlashcardSetEntity
import com.jackson4rocks.evexia.data.NoteEntity
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AccentIconButton
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.CompactEmpty
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.FlashcardSetRow
import com.jackson4rocks.evexia.ui.components.GlassSheet
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.LinearProgress
import com.jackson4rocks.evexia.ui.components.NoteRow
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.Radii
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SectionHeader
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.components.subjectColor
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.LocalReduceMotion
import com.jackson4rocks.evexia.util.DateUtils
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun StudyScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onAddNote: () -> Unit,
    onAllNotes: () -> Unit,
    onEditNote: (Long) -> Unit,
    onOpenSet: (Long) -> Unit,
    onSaveSet: (FlashcardSetEntity) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    var creatingSet by remember { mutableStateOf(false) }
    val recentNotes = state.notes.sortedByDescending { it.updatedAt }.take(3)
    val totalCards = state.flashcards.size

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        ScreenHeader(
            title = "Study",
            onBack = onBack,
            eyebrow = "Notes & flashcards",
        )
        BasicText(
            text = "${state.notes.size} notes · ${state.flashcardSets.size} sets · $totalCards cards",
            style = type.caption.copy(color = colors.textTertiary),
            modifier = Modifier.padding(horizontal = PagePadding, vertical = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            shape = Radii.hero,
            accentTint = true,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = colors.accentInk, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        text = "THE STUDY LOOP",
                        style = type.label.copy(color = colors.textSecondary),
                    )
                }
                Spacer(Modifier.height(14.dp))
                BasicText(
                    text = "Write it down.\nThen test yourself.",
                    style = type.display.copy(color = colors.textPrimary, fontSize = 28.sp, lineHeight = 31.sp),
                )
                Spacer(Modifier.height(7.dp))
                BasicText(
                    text = "Short notes and active recall beat rereading every time.",
                    style = type.bodySmall.copy(color = colors.textSecondary),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SecondaryButton(
                        label = "New note",
                        onClick = onAddNote,
                        icon = Icons.Outlined.NoteAlt,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        label = "New set",
                        onClick = { creatingSet = true },
                        icon = Icons.Filled.Add,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeader(
            title = "Recent notes",
            action = if (state.notes.isEmpty()) null else "See all",
            onAction = onAllNotes,
        )
        Spacer(Modifier.height(8.dp))
        if (recentNotes.isEmpty()) {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                contentPadding = PaddingValues(0.dp),
            ) {
                CompactEmpty(
                    title = "No notes yet",
                    message = "Capture a thought before it disappears.",
                    action = "Write one",
                    onAction = onAddNote,
                )
            }
        } else {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                contentPadding = PaddingValues(vertical = 3.dp),
            ) {
                Column {
                    recentNotes.forEachIndexed { index, note ->
                        NoteRow(
                            note = note,
                            subject = state.subject(note.subjectId),
                            onClick = { onEditNote(note.id) },
                        )
                        if (index < recentNotes.lastIndex) DividerLine()
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeader(
            title = "Flashcard sets",
            action = "New set",
            onAction = { creatingSet = true },
        )
        Spacer(Modifier.height(8.dp))
        if (state.flashcardSets.isEmpty()) {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                contentPadding = PaddingValues(0.dp),
            ) {
                CompactEmpty(
                    title = "Nothing to recall yet",
                    message = "Make a small set — five cards is plenty.",
                    action = "Create set",
                    onAction = { creatingSet = true },
                )
            }
        } else {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                contentPadding = PaddingValues(vertical = 3.dp),
            ) {
                Column {
                    state.flashcardSets.sortedByDescending { it.createdAt }.forEachIndexed { index, set ->
                        FlashcardSetRow(
                            set = set,
                            subject = state.subject(set.subjectId),
                            cardCount = state.cardsFor(set.id).size,
                            onClick = { onOpenSet(set.id) },
                        )
                        if (index < state.flashcardSets.lastIndex) DividerLine()
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            BasicText(
                text = "Knowledge compounds quietly.",
                style = type.serifItalic.copy(color = colors.textTertiary, fontSize = 16.sp),
            )
        }
    }

    if (creatingSet) {
        FlashcardSetSheet(
            initial = null,
            subjects = state.subjects,
            onDismiss = { creatingSet = false },
            onSave = {
                creatingSet = false
                onSaveSet(it)
            },
            onDelete = null,
        )
    }
}

@Composable
fun NotesScreen(
    state: AppUiState,
    subjectId: Long?,
    onBack: () -> Unit,
    onAddNote: (Long?) -> Unit,
    onEditNote: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val subject = state.subject(subjectId)
    val notes = state.notes
        .filter { subjectId == null || it.subjectId == subjectId }
        .sortedByDescending { it.updatedAt }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (subject != null) subject.name else "Notes",
            onBack = onBack,
            eyebrow = if (subject != null) "Notes in this subject" else "Everything you've written",
            actions = {
                AccentIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "New note",
                    onClick = { onAddNote(subjectId) },
                    size = 40.dp,
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        if (notes.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.NoteAlt,
                title = "The page is yours",
                message = if (subject != null) "Start a note for ${subject.name}. A few honest lines are enough."
                else "Capture a lecture, a question or a small idea. You can always shape it later.",
                actionLabel = "Write a note",
                onAction = { onAddNote(subjectId) },
            )
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = BottomContentPadding),
            ) {
                BasicText(
                    text = "${notes.size} ${if (notes.size == 1) "note" else "notes"} · newest first",
                    style = type.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(horizontal = PagePadding, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(vertical = 3.dp),
                ) {
                    Column {
                        notes.forEachIndexed { index, note ->
                            NoteRow(
                                note = note,
                                subject = state.subject(note.subjectId),
                                onClick = { onEditNote(note.id) },
                            )
                            if (index < notes.lastIndex) DividerLine()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditorScreen(
    state: AppUiState,
    noteId: Long?,
    initialSubjectId: Long?,
    onBack: () -> Unit,
    onSave: (NoteEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val existing = noteId?.let { id -> state.notes.firstOrNull { it.id == id } }
    var title by rememberSaveable(noteId) { mutableStateOf(existing?.title.orEmpty()) }
    var content by rememberSaveable(noteId) { mutableStateOf(existing?.content.orEmpty()) }
    var subjectId by rememberSaveable(noteId) { mutableStateOf(existing?.subjectId ?: initialSubjectId) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val words = remember(content) { content.trim().split(Regex("\\s+")).count { it.isNotBlank() } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (existing == null) "New note" else "Edit note",
            onBack = onBack,
            eyebrow = if (existing == null) "A thought worth keeping" else DateUtils.relativeUpdated(existing.updatedAt),
            actions = {
                if (existing != null) {
                    IconButton(
                        icon = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete note",
                        onClick = { confirmDelete = true },
                        size = 40.dp,
                        tint = colors.danger,
                    )
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PagePadding),
        ) {
            AppTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 100) title = it
                    error = null
                },
                label = "Title",
                placeholder = "What is this note about?",
                error = error,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = content,
                onValueChange = { content = it.take(20_000) },
                label = "Note",
                placeholder = "Write freely. Structure can come later…",
                singleLine = false,
                minLines = 11,
                maxLines = 24,
                imeAction = ImeAction.Default,
                supporting = "$words ${if (words == 1) "word" else "words"} · ${content.length}/20,000",
            )
            Spacer(Modifier.height(20.dp))
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
            Spacer(Modifier.height(26.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            PrimaryButton(
                label = if (existing == null) "Save note" else "Save changes",
                onClick = {
                    val clean = title.trim()
                    if (clean.isBlank()) {
                        error = "Give this note a title."
                    } else {
                        onSave(
                            NoteEntity(
                                id = existing?.id ?: 0L,
                                title = clean,
                                content = content,
                                subjectId = subjectId,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = existing?.updatedAt ?: System.currentTimeMillis(),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete this note?",
            message = "\"${existing.title}\" will be removed for good.",
            confirmLabel = "Delete note",
            onConfirm = {
                confirmDelete = false
                onDelete(existing.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun FlashcardsScreen(
    state: AppUiState,
    setId: Long,
    onBack: () -> Unit,
    onAddCard: (Long) -> Unit,
    onEditCard: (Long, Long) -> Unit,
    onSaveSet: (FlashcardSetEntity) -> Unit,
    onDeleteSet: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val set = state.flashcardSets.firstOrNull { it.id == setId }
    val subject = state.subject(set?.subjectId)
    val cards = remember(setId, state.flashcards) {
        state.flashcards.filter { it.setId == setId }.sortedBy { it.position }
    }
    var shuffledIds by remember(setId) { mutableStateOf<List<Long>?>(null) }
    var index by rememberSaveable(setId) { mutableIntStateOf(0) }
    var flipped by rememberSaveable(setId) { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf(false) }
    var confirmDeleteSet by remember { mutableStateOf(false) }
    val ordered = remember(cards, shuffledIds) {
        shuffledIds?.let { ids -> cards.sortedBy { ids.indexOf(it.id).let { p -> if (p < 0) Int.MAX_VALUE else p } } }
            ?: cards
    }
    val current = ordered.getOrNull(index.coerceIn(0, (ordered.size - 1).coerceAtLeast(0)))
    LaunchedEffect(ordered.size, index) {
        if (index >= ordered.size && ordered.isNotEmpty()) index = ordered.lastIndex
        if (ordered.isEmpty()) index = 0
    }

    if (set == null) {
        EmptyState(
            icon = Icons.Outlined.AutoStories,
            title = "Set not found",
            message = "This set may have been deleted.",
            actionLabel = "Go back",
            onAction = onBack,
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = set.title,
            onBack = onBack,
            eyebrow = subject?.name ?: "Flashcards",
            actions = {
                IconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = "Edit set",
                    onClick = { editingSet = true },
                    size = 40.dp,
                )
            },
        )
        if (cards.isEmpty()) {
            Spacer(Modifier.height(22.dp))
            EmptyState(
                icon = Icons.Outlined.Lightbulb,
                title = "Make the first card",
                message = "A question on the front, an answer on the back. Keep it small and specific.",
                actionLabel = "Add a card",
                onAction = { onAddCard(set.id) },
            )
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PagePadding, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = "${index + 1} of ${ordered.size}",
                        style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                    )
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        text = if (flipped) "Answer" else "Tap to reveal",
                        style = type.caption.copy(color = colors.textTertiary),
                    )
                }
                Spacer(Modifier.height(9.dp))
                LinearProgress(
                    progress = (index + 1f) / ordered.size,
                    color = subject?.let { subjectColor(it) } ?: colors.accent,
                )
                Spacer(Modifier.height(16.dp))
                if (current != null) {
                    StudyCard(
                        card = current,
                        flipped = flipped,
                        accent = subject?.let { subjectColor(it) } ?: colors.accent,
                        onFlip = { flipped = !flipped },
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        SecondaryButton(
                            label = "Previous",
                            onClick = {
                                if (index > 0) index-- else index = ordered.lastIndex
                                flipped = false
                            },
                            icon = Icons.Filled.ChevronLeft,
                            modifier = Modifier.weight(1f),
                            enabled = ordered.size > 1,
                        )
                        SecondaryButton(
                            label = "Reveal",
                            onClick = { flipped = !flipped },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            label = "Next",
                            onClick = {
                                if (index < ordered.lastIndex) index++ else index = 0
                                flipped = false
                            },
                            icon = Icons.Filled.ChevronRight,
                            modifier = Modifier.weight(1f),
                            enabled = ordered.size > 1,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        SecondaryButton(
                            label = "Shuffle",
                            onClick = {
                                shuffledIds = cards.map { it.id }.shuffled(Random(System.nanoTime()))
                                index = 0
                                flipped = false
                            },
                            icon = Icons.Filled.Shuffle,
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            label = "Edit card",
                            onClick = { onEditCard(set.id, current.id) },
                            icon = Icons.Filled.Edit,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        label = "Add another card",
                        onClick = { onAddCard(set.id) },
                        icon = Icons.Filled.Add,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        BasicText(
                            text = "Shuffle reshuffles the deck · reveal is free",
                            style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                        )
                    }
                }
            }
        }
    }

    if (editingSet) {
        FlashcardSetSheet(
            initial = set,
            subjects = state.subjects,
            onDismiss = { editingSet = false },
            onSave = {
                editingSet = false
                onSaveSet(it)
            },
            onDelete = {
                editingSet = false
                confirmDeleteSet = true
            },
        )
    }
    if (confirmDeleteSet) {
        ConfirmDialog(
            title = "Delete this set?",
            message = "All ${cards.size} cards in \"${set.title}\" will be deleted. This can't be undone.",
            confirmLabel = "Delete set",
            onConfirm = {
                confirmDeleteSet = false
                onDeleteSet(set.id)
            },
            onDismiss = { confirmDeleteSet = false },
        )
    }
}

@Composable
private fun StudyCard(
    card: FlashcardEntity,
    flipped: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onFlip: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(440),
        label = "cardFlip",
    )
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 350.dp, max = 460.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            }
            .pressable(onClick = onFlip),
        shape = RoundedCornerShape(30.dp),
        strong = true,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (flipped) accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                .graphicsLayer { if (rotation > 90f) rotationY = 180f }
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = if (flipped) "ANSWER" else "QUESTION",
                    style = type.label.copy(color = if (flipped) colors.accentInk else colors.textTertiary),
                )
                Spacer(Modifier.height(18.dp))
                BasicText(
                    text = if (flipped) card.answer.ifBlank { "No answer written yet." } else card.question,
                    style = type.serif.copy(
                        color = colors.textPrimary,
                        fontSize = 25.sp,
                        lineHeight = 32.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (flipped) accent.copy(alpha = 0.4f) else colors.glassQuiet),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (flipped) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FlashcardEditorScreen(
    state: AppUiState,
    setId: Long,
    cardId: Long?,
    onBack: () -> Unit,
    onSave: (FlashcardEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val set = state.flashcardSets.firstOrNull { it.id == setId }
    val existing = cardId?.let { id -> state.flashcards.firstOrNull { it.id == id } }
    var question by rememberSaveable(cardId) { mutableStateOf(existing?.question.orEmpty()) }
    var answer by rememberSaveable(cardId) { mutableStateOf(existing?.answer.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nextPosition = remember(state.flashcards, setId) {
        (state.flashcards.filter { it.setId == setId }.maxOfOrNull { it.position } ?: -1) + 1
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (existing == null) "New card" else "Edit card",
            onBack = onBack,
            eyebrow = set?.title ?: "Flashcards",
            actions = {
                if (existing != null) {
                    IconButton(
                        icon = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete card",
                        onClick = { confirmDelete = true },
                        size = 40.dp,
                        tint = colors.danger,
                    )
                }
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PagePadding),
        ) {
            AppTextField(
                value = question,
                onValueChange = {
                    if (it.length <= 500) question = it
                    error = null
                },
                label = "Question · front",
                placeholder = "What do you need to remember?",
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                error = error,
                supporting = "${question.length}/500",
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(14.dp))
            AppTextField(
                value = answer,
                onValueChange = { if (it.length <= 1000) answer = it },
                label = "Answer · back",
                placeholder = "The clear, short version",
                singleLine = false,
                minLines = 4,
                maxLines = 10,
                supporting = "${answer.length}/1,000",
                imeAction = ImeAction.Done,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.glassQuiet)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    null,
                    tint = colors.accentInk,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicText(
                    text = "Good cards ask one thing at a time. Write the answer as if you're explaining it to a friend.",
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
            Spacer(Modifier.height(26.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            PrimaryButton(
                label = if (existing == null) "Add card" else "Save card",
                onClick = {
                    val q = question.trim()
                    if (q.isBlank()) {
                        error = "A card needs a question."
                    } else {
                        onSave(
                            FlashcardEntity(
                                id = existing?.id ?: 0L,
                                setId = setId,
                                question = q,
                                answer = answer.trim(),
                                position = existing?.position ?: nextPosition,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete this card?",
            message = "The card and its answer will be removed from the set.",
            confirmLabel = "Delete card",
            onConfirm = {
                confirmDelete = false
                onDelete(existing.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FlashcardSetSheet(
    initial: FlashcardSetEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (FlashcardSetEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = DF.colors
    val type = DF.type
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable(initial?.id) { mutableStateOf(initial?.description.orEmpty()) }
    var subjectId by rememberSaveable(initial?.id) { mutableStateOf(initial?.subjectId) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassSheet(
        title = if (initial == null) "New flashcard set" else "Edit set",
        subtitle = "Group cards around one topic or exam.",
        onDismiss = onDismiss,
    ) {
        AppTextField(
            value = title,
            onValueChange = {
                if (it.length <= 70) title = it
                error = null
            },
            label = "Set name",
            placeholder = "e.g. Cell biology essentials",
            error = error,
            supporting = "${title.length}/70",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = description,
            onValueChange = { if (it.length <= 240) description = it },
            label = "Description (optional)",
            placeholder = "What does this set cover?",
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            supporting = "${description.length}/240",
            imeAction = ImeAction.Done,
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
            label = if (initial == null) "Create set" else "Save set",
            onClick = {
                val clean = title.trim()
                if (clean.isBlank()) {
                    error = "Give this set a name."
                } else {
                    onSave(
                        FlashcardSetEntity(
                            id = initial?.id ?: 0L,
                            title = clean,
                            description = description.trim(),
                            subjectId = subjectId,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (onDelete != null) {
            Spacer(Modifier.height(9.dp))
            SecondaryButton(
                label = "Delete set",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                danger = true,
                icon = Icons.Filled.DeleteOutline,
            )
        }
    }
}

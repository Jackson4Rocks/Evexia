package com.jackson4rocks.evexia.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.data.TimetableEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AccentIconButton
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.ClassRow
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.ScreenTitle
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SegmentedControl
import com.jackson4rocks.evexia.ui.components.isNextClass
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.components.subjectColor
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.DateUtils
import com.jackson4rocks.evexia.util.showTimePicker
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TimetableScreen(
    state: AppUiState,
    onAddClass: (Int) -> Unit,
    onEditClass: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val prefs = state.preferences
    val days = remember(prefs.weekStartsMonday, prefs.showWeekends) {
        DateUtils.orderedDays(prefs.weekStartsMonday).filter { prefs.showWeekends || it <= 5 }
    }
    var selectedDay by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    val activeDay = if (selectedDay in days) selectedDay else days.first()
    var weekView by rememberSaveable { mutableStateOf(false) }
    val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
    val todayClasses = state.timetable.filter { it.dayOfWeek == LocalDate.now().dayOfWeek.value }
    val dayClasses = state.timetable
        .filter { it.dayOfWeek == activeDay }
        .sortedBy { it.startMinutes }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "Timetable",
            eyebrow = "The shape of your week",
            subtitle = "${state.timetable.size} ${if (state.timetable.size == 1) "class" else "classes"} · ${todayClasses.size} today",
            trailing = {
                AccentIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add a class",
                    onClick = { onAddClass(activeDay) },
                    size = 40.dp,
                )
            },
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            days.forEach { day ->
                val selected = activeDay == day && !weekView
                val count = state.timetable.count { it.dayOfWeek == day }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (selected) colors.accent else colors.glassQuiet)
                        .border(
                            1.dp,
                            if (selected) Color.Transparent else colors.border,
                            RoundedCornerShape(17.dp),
                        )
                        .pressable {
                            weekView = false
                            selectedDay = day
                        }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        text = DateUtils.dayInitial(day),
                        style = type.cardTitle.copy(
                            color = if (selected) colors.onAccent else colors.textPrimary,
                            fontSize = 13.sp,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(count.coerceAtMost(3)) {
                            Box(
                                Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) colors.onAccent.copy(alpha = 0.55f) else colors.accentInk.copy(alpha = 0.6f)),
                            )
                        }
                        if (count == 0) {
                            Box(
                                Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (DateUtils.isToday(day)) colors.accentInk else colors.borderStrong),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Box(Modifier.padding(horizontal = PagePadding)) {
            SegmentedControl(
                items = listOf("Day", "Week"),
                selectedIndex = if (weekView) 1 else 0,
                onSelected = { weekView = it == 1 },
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = BottomContentPadding + 8.dp),
        ) {
            if (!weekView) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = DateUtils.dayName(activeDay) + if (DateUtils.isToday(activeDay)) " · Today" else "",
                        style = type.sectionTitle.copy(color = colors.textPrimary),
                    )
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        text = if (dayClasses.isEmpty()) "Open" else "${dayClasses.sumOf { it.endMinutes - it.startMinutes } / 60}h ${dayClasses.sumOf { it.endMinutes - it.startMinutes } % 60}m",
                        style = type.caption.copy(color = colors.textTertiary),
                    )
                }
                Spacer(Modifier.height(9.dp))
                if (dayClasses.isEmpty()) {
                    GlassSurface(
                        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "A clear ${DateUtils.dayName(activeDay).lowercase()}",
                            message = "No classes yet. Add one and Evexia will keep it in your weekly rhythm.",
                            actionLabel = "Add a class",
                            onAction = { onAddClass(activeDay) },
                        )
                    }
                } else {
                    GlassSurface(
                        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        Column {
                            dayClasses.forEachIndexed { index, entry ->
                                val status = if (DateUtils.isToday(activeDay)) {
                                    val (current, _) = com.jackson4rocks.evexia.ui.components.classStatus(entry, nowMinutes)
                                    current
                                } else false
                                val next = if (DateUtils.isToday(activeDay)) isNextClass(dayClasses, entry, nowMinutes) else false
                                ClassRow(
                                    entry = entry,
                                    subject = state.subject(entry.subjectId),
                                    current = status,
                                    next = next,
                                    onClick = { onEditClass(entry.id) },
                                )
                                if (index < dayClasses.lastIndex) DividerLine()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    BasicText(
                        text = "Repeats every week · tap a class to edit",
                        style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                    )
                }
            } else {
                val ordered = days.map { day -> day to state.timetable.filter { it.dayOfWeek == day }.sortedBy { it.startMinutes } }
                ordered.forEach { (day, classes) ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = DateUtils.dayName(day),
                            style = type.cardTitle.copy(
                                color = if (DateUtils.isToday(day)) colors.accentInk else colors.textPrimary,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        if (DateUtils.isToday(day)) {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(colors.accentSoft)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                BasicText("TODAY", style = type.label.copy(color = colors.accentInk, fontSize = 7.5.sp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        BasicText(
                            text = if (classes.isEmpty()) "Free" else "${classes.size} ${if (classes.size == 1) "class" else "classes"}",
                            style = type.caption.copy(color = colors.textTertiary),
                        )
                        IconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "Add class on ${DateUtils.dayName(day)}",
                            onClick = { onAddClass(day) },
                            size = 30.dp,
                            glass = false,
                        )
                    }
                    if (classes.isEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PagePadding)
                                .clip(RoundedCornerShape(15.dp))
                                .background(colors.glassQuiet.copy(alpha = 0.4f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            BasicText("Nothing scheduled — space to breathe.", style = type.caption.copy(color = colors.textTertiary))
                        }
                    } else {
                        GlassSurface(
                            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                            contentPadding = PaddingValues(vertical = 3.dp),
                            elevated = false,
                        ) {
                            Column {
                                classes.forEachIndexed { index, entry ->
                                    ClassRow(
                                        entry = entry,
                                        subject = state.subject(entry.subjectId),
                                        current = DateUtils.isToday(day) &&
                                            com.jackson4rocks.evexia.ui.components.classStatus(entry, nowMinutes).first,
                                        next = DateUtils.isToday(day) && isNextClass(classes, entry, nowMinutes),
                                        onClick = { onEditClass(entry.id) },
                                    )
                                    if (index < classes.lastIndex) DividerLine()
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun TimetableEditorScreen(
    state: AppUiState,
    entryId: Long?,
    initialDay: Int,
    onBack: () -> Unit,
    onSave: (TimetableEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val existing = entryId?.let { id -> state.timetable.firstOrNull { it.id == id } }
    var title by rememberSaveable(entryId) { mutableStateOf(existing?.title.orEmpty()) }
    var subjectId by rememberSaveable(entryId) { mutableStateOf(existing?.subjectId) }
    var day by rememberSaveable(entryId) { mutableIntStateOf(existing?.dayOfWeek ?: initialDay) }
    var start by rememberSaveable(entryId) { mutableIntStateOf(existing?.startMinutes ?: 9 * 60) }
    var end by rememberSaveable(entryId) { mutableIntStateOf(existing?.endMinutes ?: 10 * 60) }
    var location by rememberSaveable(entryId) { mutableStateOf(existing?.location.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val days = remember(state.preferences.weekStartsMonday) {
        DateUtils.orderedDays(state.preferences.weekStartsMonday)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (existing == null) "New class" else "Edit class",
            onBack = onBack,
            eyebrow = "Your weekly rhythm",
            actions = {
                if (existing != null) {
                    IconButton(
                        icon = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete class",
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
                    if (it.length <= 70) title = it
                    error = null
                },
                label = "Class",
                placeholder = "e.g. Linear Algebra",
                error = error,
                supporting = "${title.length}/70",
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            )
            Spacer(Modifier.height(14.dp))
            AppTextField(
                value = location,
                onValueChange = { if (it.length <= 70) location = it },
                label = "Room or location (optional)",
                placeholder = "e.g. Science Block, Room 204",
                leadingIcon = Icons.Outlined.LocationOn,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
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
            Spacer(Modifier.height(20.dp))
            BasicText(
                "Day",
                style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(days) { value ->
                    AppChip(
                        label = DateUtils.dayName(value).take(3),
                        selected = day == value,
                        onClick = { day = value },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            BasicText(
                "Time",
                style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    label = DateUtils.timeLabel(context, start),
                    onClick = { showTimePicker(context, start) { start = it; error = null } },
                    icon = Icons.Outlined.Schedule,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    label = DateUtils.timeLabel(context, end),
                    onClick = { showTimePicker(context, end) { end = it; error = null } },
                    icon = Icons.Outlined.Schedule,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = if (end > start) {
                    val duration = end - start
                    "Lasts ${duration / 60}h${if (duration % 60 == 0) "" else " ${duration % 60}m"}"
                } else "End time must be after the start time",
                style = type.caption.copy(
                    color = if (end > start) colors.textTertiary else colors.danger,
                    fontSize = 10.sp,
                ),
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
            BasicText(
                "Class colour comes from its subject.",
                style = type.caption.copy(color = colors.textTertiary),
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding, vertical = 12.dp)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                label = if (existing == null) "Add to timetable" else "Save class",
                onClick = {
                    val clean = title.trim()
                    if (clean.isBlank()) {
                        error = "Give this class a name."
                    } else if (end <= start) {
                        error = "Choose an end time after the start."
                    } else {
                        error = null
                        onSave(
                            TimetableEntity(
                                id = existing?.id ?: 0L,
                                title = clean,
                                subjectId = subjectId,
                                dayOfWeek = day,
                                startMinutes = start,
                                endMinutes = end,
                                location = location.trim(),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
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
            title = "Remove this class?",
            message = "\"${existing.title}\" will disappear from your timetable. Tasks and notes are untouched.",
            confirmLabel = "Remove class",
            onConfirm = {
                confirmDelete = false
                onDelete(existing.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

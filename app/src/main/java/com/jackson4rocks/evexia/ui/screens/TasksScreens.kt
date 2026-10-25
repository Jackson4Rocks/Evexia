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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.data.TaskEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AccentIconButton
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.CompactEmpty
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.EmptyState
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.ScreenTitle
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SegmentedControl
import com.jackson4rocks.evexia.ui.components.TaskRow
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.DateUtils
import com.jackson4rocks.evexia.util.showDatePicker
import com.jackson4rocks.evexia.util.showTimePicker
import java.time.LocalDate

private val filters = listOf("Today", "Upcoming", "Overdue", "Done", "All")

@Composable
fun TasksScreen(
    state: AppUiState,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    var filter by rememberSaveable { mutableStateOf(0) }
    val today = LocalDate.now().toEpochDay()
    val prefs = state.preferences
    val filtered = remember(state.tasks, filter, prefs.showCompletedTasks) {
        when (filter) {
            0 -> state.tasks.filter { it.dueDate == today }.sortedWith(taskComparator())
            1 -> state.tasks.filter { !it.completed && it.dueDate != null && it.dueDate > today }
                .sortedWith(taskComparator())
            2 -> state.tasks.filter {
                !it.completed && it.dueDate != null && it.dueDate < today
            }.sortedWith(taskComparator())
            3 -> state.tasks.filter { it.completed }.sortedByDescending { it.completedAt ?: 0L }
            else -> state.tasks.sortedWith(taskComparator())
        }
    }.filter { prefs.showCompletedTasks || !it.completed || filter == 3 }
    val openCount = state.tasks.count { !it.completed }
    val overdueCount = state.tasks.count {
        !it.completed && DateUtils.isOverdue(it.dueDate, it.dueMinutes, false)
    }
    val done = state.tasks.count { it.completed }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            title = "Tasks",
            eyebrow = "On your desk",
            subtitle = "$openCount open${if (overdueCount > 0) " · $overdueCount overdue" else " · $done finished"}",
            trailing = {
                AccentIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add task",
                    onClick = onAddTask,
                    size = 40.dp,
                )
            },
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters.size) { index ->
                AppChip(
                    label = filters[index],
                    selected = filter == index,
                    onClick = { filter = index },
                    dotColor = if (index == 2 && overdueCount > 0) colors.danger else null,
                )
            }
        }
        Spacer(Modifier.height(15.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = BottomContentPadding + 10.dp),
        ) {
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = when (filter) {
                        2 -> Icons.Outlined.TaskAlt
                        3 -> Icons.Outlined.CheckCircle
                        else -> Icons.Outlined.Schedule
                    },
                    title = when (filter) {
                        0 -> "Nothing due today"
                        1 -> "The horizon is clear"
                        2 -> "No overdue work"
                        3 -> "No finished tasks yet"
                        else -> "A blank list"
                    },
                    message = when (filter) {
                        2 -> "You're right on top of things. Keep it that way."
                        3 -> "Finish a task and it will collect here."
                        else -> "Add a deadline, a small next step or something you keep forgetting."
                    },
                    actionLabel = if (filter == 3) null else "Add a task",
                    onAction = if (filter == 3) null else onAddTask,
                )
            } else {
                GlassSurface(
                    Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(vertical = 3.dp),
                ) {
                    Column {
                        filtered.forEachIndexed { index, task ->
                            TaskRow(
                                task = task,
                                subject = state.subject(task.subjectId),
                                onToggle = { onToggleTask(task) },
                                onClick = { onEditTask(task.id) },
                            )
                            if (index < filtered.lastIndex) DividerLine()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = when (filter) {
                            0 -> "A task without a time still belongs to the day"
                            1 -> "Looking ahead is a kindness to your future self"
                            2 -> "Overdue simply means it needs a new decision"
                            3 -> "Quiet progress adds up"
                            else -> "Tap the circle to finish · tap the row to edit"
                        },
                        style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                    )
                }
            }
        }
    }
}

private fun taskComparator(): Comparator<TaskEntity> = compareBy<TaskEntity>(
    { it.completed },
    { it.dueDate ?: Long.MAX_VALUE },
    { it.dueMinutes ?: 24 * 60 },
    { -it.priority },
)

@Composable
fun TaskEditorScreen(
    state: AppUiState,
    taskId: Long?,
    initialSubjectId: Long?,
    onBack: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val existing = taskId?.let { id -> state.tasks.firstOrNull { it.id == id } }
    var title by rememberSaveable(taskId) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(taskId) { mutableStateOf(existing?.description.orEmpty()) }
    var subjectId by rememberSaveable(taskId) {
        mutableStateOf(existing?.subjectId ?: initialSubjectId)
    }
    var dueDate by rememberSaveable(taskId) { mutableStateOf(existing?.dueDate) }
    var dueMinutes by rememberSaveable(taskId) { mutableStateOf(existing?.dueMinutes) }
    var priority by rememberSaveable(taskId) { mutableStateOf(existing?.priority ?: 1) }
    var recurrence by rememberSaveable(taskId) { mutableStateOf(existing?.recurrence ?: "NONE") }
    var reminder by rememberSaveable(taskId) { mutableStateOf(existing?.reminderMinutes) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var taskCompleted by rememberSaveable(taskId) { mutableStateOf(existing?.completed ?: false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (existing == null) "New task" else "Edit task",
            onBack = onBack,
            eyebrow = "A clear next step",
            actions = {
                if (existing != null) {
                    com.jackson4rocks.evexia.ui.components.IconButton(
                        icon = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete task",
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
                    if (it.length <= 90) title = it
                    error = null
                },
                label = "Task",
                placeholder = "What needs to happen?",
                error = error,
                supporting = "${title.length}/90",
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(14.dp))
            AppTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = "Details (optional)",
                placeholder = "Notes, steps or context",
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                imeAction = ImeAction.Done,
                supporting = "${description.length}/500",
            )
            Spacer(Modifier.height(20.dp))
            FieldLabel("Subject")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    AppChip("No subject", subjectId == null, onClick = { subjectId = null })
                }
                items(state.subjects, key = { it.id }) { subject ->
                    AppChip(
                        label = subject.name,
                        selected = subjectId == subject.id,
                        onClick = { subjectId = subject.id },
                        dotColor = SubjectDot(subject),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            FieldLabel("Due date")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val dateLabel = dueDate?.let {
                    DateUtils.formatDue(context, it, null)
                } ?: "No date set"
                SecondaryButton(
                    label = dateLabel,
                    onClick = {
                        showDatePicker(context, dueDate?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()) {
                            dueDate = it.toEpochDay()
                            if (reminder == null && state.preferences.remindersEnabled) {
                                reminder = state.preferences.reminderLead
                            }
                        }
                    },
                    icon = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.weight(1f),
                )
                if (dueDate != null) {
                    com.jackson4rocks.evexia.ui.components.IconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Clear due date",
                        onClick = { dueDate = null; dueMinutes = null; reminder = null },
                        size = 50.dp,
                    )
                }
            }
            if (dueDate != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SecondaryButton(
                        label = dueMinutes?.let { DateUtils.timeLabel(context, it) } ?: "Add a time",
                        onClick = {
                            showTimePicker(context, dueMinutes ?: 9 * 60) { dueMinutes = it }
                        },
                        icon = Icons.Outlined.Schedule,
                        modifier = Modifier.weight(1f),
                    )
                    if (dueMinutes != null) {
                        com.jackson4rocks.evexia.ui.components.IconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = "Clear time",
                            onClick = { dueMinutes = null },
                            size = 50.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            FieldLabel("Priority")
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                items = listOf("Low", "Normal", "High"),
                selectedIndex = priority,
                onSelected = { priority = it },
            )
            Spacer(Modifier.height(20.dp))
            FieldLabel("Repeat")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("NONE" to "One time", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly")) { pair ->
                    AppChip(pair.second, recurrence == pair.first, onClick = { recurrence = pair.first })
                }
            }
            Spacer(Modifier.height(20.dp))
            FieldLabel("Reminder")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val options = listOf<Pair<Int?, String>>(
                    null to "Off",
                    10 to "10 min before",
                    30 to "30 min before",
                    60 to "1 hour before",
                    24 * 60 to "1 day before",
                )
                items(options) { option ->
                    AppChip(
                        label = option.second,
                        selected = reminder == option.first,
                        onClick = {
                            reminder = option.first
                            if (option.first != null && dueDate == null) {
                                dueDate = LocalDate.now().toEpochDay()
                            }
                        },
                    )
                }
            }
            if (existing != null) {
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp))
                        .background(colors.glassQuiet)
                        .borderAll(colors.border)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = if (taskCompleted) "Task complete" else "Mark as complete",
                            style = type.cardTitle.copy(color = colors.textPrimary),
                        )
                        BasicText(
                            text = if (taskCompleted) "Tap to move it back to open work" else "Finished is a fine place to be",
                            style = type.caption.copy(color = colors.textSecondary),
                        )
                    }
                    com.jackson4rocks.evexia.ui.components.AppSwitch(checked = taskCompleted, onCheckedChange = { taskCompleted = it })
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding, vertical = 12.dp)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                label = if (existing == null) "Add task" else "Save task",
                onClick = {
                    val clean = title.trim()
                    if (clean.isBlank()) {
                        error = "Give this task a name."
                    } else if (reminder != null && dueDate == null) {
                        error = "Add a due date for the reminder."
                    } else {
                        onSave(
                            TaskEntity(
                                id = existing?.id ?: 0L,
                                title = clean,
                                description = description.trim(),
                                subjectId = subjectId,
                                dueDate = dueDate,
                                dueMinutes = dueMinutes,
                                priority = priority,
                                completed = taskCompleted,
                                recurrence = recurrence,
                                reminderMinutes = reminder,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                completedAt = if (taskCompleted) (existing?.completedAt ?: System.currentTimeMillis()) else null,
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
            title = "Delete this task?",
            message = "\"${existing.title}\" will be removed. This can't be undone.",
            confirmLabel = "Delete task",
            onConfirm = {
                confirmDelete = false
                onDelete(existing.id)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    BasicText(
        text = text,
        style = DF.type.caption.copy(color = DF.colors.textSecondary, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SubjectDot(subject: SubjectEntity): androidx.compose.ui.graphics.Color =
    com.jackson4rocks.evexia.ui.components.subjectColor(subject)

private fun Modifier.borderAll(color: androidx.compose.ui.graphics.Color): Modifier =
    this.border(1.dp, color, RoundedCornerShape(17.dp))

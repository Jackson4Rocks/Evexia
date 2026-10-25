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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.data.TaskEntity
import com.jackson4rocks.evexia.data.TimetableEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AnimatedCheck
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.ClassRow
import com.jackson4rocks.evexia.ui.components.CompactEmpty
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.FileGlyph
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.PersonAvatar
import com.jackson4rocks.evexia.ui.components.ProgressRing
import com.jackson4rocks.evexia.ui.components.Radii
import com.jackson4rocks.evexia.ui.components.SectionHeader
import com.jackson4rocks.evexia.ui.components.StudentIdCard
import com.jackson4rocks.evexia.ui.components.TaskRow
import com.jackson4rocks.evexia.ui.components.isNextClass
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.DateUtils
import com.jackson4rocks.evexia.util.FileStore
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: AppUiState,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onTasks: () -> Unit,
    onTimetable: () -> Unit,
    onStudy: () -> Unit,
    onLibrary: () -> Unit,
    onOpenLibraryItem: (LibraryItemEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (Long) -> Unit,
    onEditClass: (Long) -> Unit,
    onAddTask: () -> Unit,
    onAddSubject: () -> Unit,
    onAddNote: () -> Unit,
    onAddClass: () -> Unit,
    onScan: () -> Unit,
    onConvert: () -> Unit,
    onPrint: () -> Unit,
    onFlowAi: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val prefs = state.preferences
    val nowMinutes = rememberNowMinutes()
    val today = LocalDate.now()
    val todayClasses = state.timetable
        .filter { it.dayOfWeek == today.dayOfWeek.value }
        .sortedBy { it.startMinutes }
    val todayTasks = state.tasks.filter { it.dueDate == today.toEpochDay() }
    val completedToday = todayTasks.count { it.completed }
    val progress = if (todayTasks.isEmpty()) 0f else completedToday.toFloat() / todayTasks.size
    val upcomingTasks = state.tasks
        .filter { !it.completed }
        .filter { it.dueDate == null || it.dueDate <= today.plusDays(7).toEpochDay() }
        .sortedWith(compareBy<TaskEntity>({ it.dueDate ?: Long.MAX_VALUE }, { it.dueMinutes ?: 24 * 60 }, { -it.priority }))
        .take(4)
    val nextClass = todayClasses.firstOrNull { it.startMinutes > nowMinutes }
    val displayName = prefs.displayName.trim().ifBlank { "there" }.substringBefore(' ')
    val recent = state.recentLibrary.filter { it.lastOpenedAt != null }.take(4)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = BottomContentPadding),
    ) {
        // Brand and personal controls
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(31.dp)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                com.jackson4rocks.evexia.ui.components.BrandMark(size = 31.dp)
            }
            Spacer(Modifier.width(9.dp))
            BasicText(
                text = "Evexia",
                style = type.cardTitle.copy(
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                ),
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                onClick = onSettings,
                size = 38.dp,
                glass = false,
            )
            Spacer(Modifier.width(4.dp))
            PersonAvatar(
                name = prefs.displayName,
                path = prefs.profileImagePath,
                size = 38.dp,
                modifier = Modifier.pressable(onClick = onProfile),
            )
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            BasicText(
                text = DateUtils.longDate().uppercase(),
                style = type.label.copy(color = colors.textTertiary),
            )
            Spacer(Modifier.height(9.dp))
            BasicText(
                text = DateUtils.greeting() + ",",
                style = type.hero.copy(color = colors.textPrimary, fontSize = 44.sp, lineHeight = 46.sp),
            )
            BasicText(
                text = "$displayName.",
                style = type.serifItalic.copy(
                    color = colors.accentInk,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                ),
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = when {
                    todayClasses.isNotEmpty() && todayTasks.isNotEmpty() -> "A full day. Your next moves are ready below."
                    todayClasses.isNotEmpty() -> "Classes are lined up. Keep the rest of the day light."
                    todayTasks.isNotEmpty() -> "No classes today — a good window to make progress."
                    else -> "A quiet page today. Make room for one good idea."
                },
                style = type.bodySmall.copy(color = colors.textSecondary),
            )
        }

        Spacer(Modifier.height(20.dp))

        StudentIdCard(
            preferences = prefs,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            onClick = onProfile,
        )

        Spacer(Modifier.height(20.dp))

        // Focus card
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = Radii.hero,
            accentTint = true,
            contentPadding = PaddingValues(20.dp),
            onClick = onTimetable,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = "TODAY'S RHYTHM",
                            style = type.label.copy(color = colors.textSecondary),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            BasicText(
                                text = if (todayTasks.isEmpty()) "—" else "${(progress * 100).toInt()}%",
                                style = type.numeric.copy(color = colors.textPrimary, fontSize = 58.sp, lineHeight = 56.sp),
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicText(
                                text = if (todayTasks.isEmpty()) "nothing due" else "$completedToday of ${todayTasks.size} done",
                                style = type.caption.copy(color = colors.textSecondary),
                                modifier = Modifier.padding(bottom = 7.dp),
                            )
                        }
                    }
                    ProgressRing(
                        progress = if (todayTasks.isEmpty()) 0f else progress,
                        size = 78.dp,
                        stroke = 7.dp,
                        color = colors.accentInk,
                        trackColor = Color.White.copy(alpha = 0.28f),
                    ) {
                        Icon(
                            imageVector = if (progress >= 1f && todayTasks.isNotEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.Description,
                            contentDescription = null,
                            tint = colors.accentInk,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(27.dp)
                            .clip(CircleShape)
                            .background(colors.glassQuiet),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    BasicText(
                        text = when {
                            nextClass != null -> "Next · ${nextClass.title} at ${DateUtils.timeLabel(androidx.compose.ui.platform.LocalContext.current, nextClass.startMinutes)}"
                            todayClasses.isNotEmpty() -> "That's your last class for today"
                            else -> "No classes on the timetable today"
                        },
                        style = type.caption.copy(color = colors.textSecondary),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.Filled.ArrowForward,
                        null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quick actions, horizontally registerable
        SectionHeader(title = "Quick Tools")
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { QuickAction("Evexia AI", "Ask & summarise", Icons.Outlined.AutoAwesome, colors.accentInk, onFlowAi, featured = true) }
            item { QuickAction("Scan", "Paper → PDF", Icons.Outlined.CameraAlt, colors.accentInk, onScan, featured = true) }
            item { QuickAction("Studio", "PDF & print", Icons.Outlined.PictureAsPdf, Color(0xFF8E7BEF), onConvert, featured = true) }
            item { QuickAction("Print", "Paper or PDF", Icons.Outlined.Print, Color(0xFF5D8E83), onPrint) }
            item { QuickAction("Task", "A new to-do", Icons.Outlined.CheckCircle, Color(0xFFD9825D), onAddTask) }
            item { QuickAction("Note", "Capture it", Icons.Outlined.NoteAlt, Color(0xFF5F9CC9), onAddNote) }
            item { QuickAction("Subject", "New folder", Icons.Outlined.FolderOpen, Color(0xFF4E9273), onAddSubject) }
            item { QuickAction("Class", "Add to week", Icons.Outlined.CalendarMonth, Color(0xFFB68E35), onAddClass) }
        }

        Spacer(Modifier.height(26.dp))

        SectionHeader(
            title = "Today's classes",
            action = "Full week",
            onAction = onTimetable,
        )
        Spacer(Modifier.height(10.dp))
        GlassSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 3.dp),
        ) {
            Column {
                if (todayClasses.isEmpty()) {
                    CompactEmpty(
                        title = "Nothing scheduled",
                        message = "Add your first class and the week will take shape.",
                        action = "Add",
                        onAction = onAddClass,
                    )
                } else {
                    todayClasses.take(5).forEachIndexed { index, entry ->
                        val subject = state.subject(entry.subjectId)
                        ClassRow(
                            entry = entry,
                            subject = subject,
                            current = nowMinutes in entry.startMinutes until entry.endMinutes,
                            next = isNextClass(todayClasses, entry, nowMinutes),
                            onClick = { onEditClass(entry.id) },
                        )
                        if (index < todayClasses.take(5).lastIndex) DividerLine()
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        SectionHeader(
            title = "On your desk",
            action = "All tasks",
            onAction = onTasks,
        )
        Spacer(Modifier.height(10.dp))
        GlassSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 3.dp),
        ) {
            Column {
                if (upcomingTasks.isEmpty()) {
                    CompactEmpty(
                        title = "The desk is clear",
                        message = if (state.tasks.any { it.completed }) "Everything due soon is finished." else "Add a task when something lands on your plate.",
                        action = "New task",
                        onAction = onAddTask,
                    )
                } else {
                    upcomingTasks.forEachIndexed { index, task ->
                        TaskRow(
                            task = task,
                            subject = state.subject(task.subjectId),
                            onToggle = { onToggleTask(task) },
                            onClick = { onEditTask(task.id) },
                        )
                        if (index < upcomingTasks.lastIndex) DividerLine()
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionHeader(title = "Pick up where you left off", action = "Library", onAction = onLibrary)
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(recent, key = { it.id }) { item ->
                    RecentItemCard(item = item, onClick = { onOpenLibraryItem(item) })
                }
            }
        } else {
            Spacer(Modifier.height(26.dp))
            SectionHeader(title = "Study shelf", action = "Open", onAction = onStudy)
            Spacer(Modifier.height(10.dp))
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                CompactEmpty(
                    title = "Your materials live here",
                    message = "Notes, flashcards and files stay on this device, ready when you are.",
                    action = "Browse",
                    onAction = onStudy,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(colors.success))
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = "All changes saved on this device",
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    caption: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    featured: Boolean = false,
) {
    val colors = DF.colors
    val type = DF.type
    GlassSurface(
        modifier = Modifier.width(128.dp),
        shape = RoundedCornerShape(22.dp),
        tint = if (featured) color.copy(alpha = if (colors.dark) 0.16f else 0.13f) else colors.glass,
        contentPadding = PaddingValues(13.dp),
        onClick = onClick,
        elevated = false,
    ) {
        Column(Modifier.height(85.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = if (featured) 0.24f else 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                text = label,
                style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.5.sp),
            )
            BasicText(
                text = caption,
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RecentItemCard(item: LibraryItemEntity, onClick: () -> Unit) {
    val colors = DF.colors
    val type = DF.type
    val isUrl = item.kind == LibraryItemEntity.KIND_BOOKMARK
    val missing = !isUrl && !File(item.location).exists()
    GlassSurface(
        modifier = Modifier.width(188.dp),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(13.dp),
        onClick = onClick,
        elevated = false,
    ) {
        Column(Modifier.height(92.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileGlyph(
                    mimeType = item.mimeType,
                    isBookmark = isUrl,
                    tint = if (missing) colors.danger else null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.weight(1f))
                BasicText(
                    text = if (isUrl) "LINK" else item.title.substringAfterLast('.', "").uppercase().take(4),
                    style = type.label.copy(color = colors.textTertiary, fontSize = 8.sp),
                )
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                text = item.title,
                style = type.cardTitle.copy(color = if (missing) colors.danger else colors.textPrimary, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = if (missing) "Unavailable" else if (isUrl) "Saved link" else FileStore.humanSize(item.sizeBytes),
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun rememberNowMinutes(): Int {
    var value by remember { mutableIntStateOf(LocalTime.now().hour * 60 + LocalTime.now().minute) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            value = LocalTime.now().hour * 60 + LocalTime.now().minute
        }
    }
    return value
}

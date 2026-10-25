package com.jackson4rocks.evexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.FlashcardSetEntity
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.data.NoteEntity
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.data.TaskEntity
import com.jackson4rocks.evexia.data.TimetableEntity
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.SubjectPalette
import com.jackson4rocks.evexia.util.DateUtils
import com.jackson4rocks.evexia.util.FileStore
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

fun subjectColor(subject: SubjectEntity?): Color =
    subject?.color?.let { Color(it) } ?: SubjectPalette.first()

fun Color.readableInk(): Color =
    if (this.luminance() > 0.52f) Color(0xFF171916) else Color(0xFFF9F6EE)

@Composable
fun TaskRow(
    task: TaskEntity,
    subject: SubjectEntity?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    val overdue = DateUtils.isOverdue(task.dueDate, task.dueMinutes, task.completed)
    val dueColor = if (overdue) colors.danger else colors.textTertiary
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AnimatedCheck(
            checked = task.completed,
            onToggle = onToggle,
            size = 25.dp,
            modifier = Modifier.padding(top = 1.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = task.title,
                style = type.cardTitle.copy(
                    color = if (task.completed) colors.textTertiary else colors.textPrimary,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (task.description.isNotBlank() && !task.completed) {
                BasicText(
                    text = task.description,
                    style = type.caption.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (task.dueDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Schedule,
                            null,
                            tint = dueColor,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = DateUtils.formatDue(
                                androidx.compose.ui.platform.LocalContext.current,
                                task.dueDate,
                                task.dueMinutes,
                            ),
                            style = type.caption.copy(color = dueColor, fontSize = 10.5.sp),
                        )
                    }
                }
                if (subject != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(subjectColor(subject)),
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = subject.name,
                            style = type.caption.copy(color = colors.textTertiary, fontSize = 10.5.sp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (!task.completed && task.priority == 2) {
            Box(
                Modifier
                    .padding(start = 8.dp, top = 5.dp)
                    .clip(CircleShape)
                    .background(colors.dangerSoft)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                BasicText(
                    text = "HIGH",
                    style = type.label.copy(color = colors.danger, fontSize = 8.sp),
                )
            }
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(DF.colors.border.copy(alpha = 0.55f)),
    )
}

@Composable
fun ClassRow(
    entry: TimetableEntity,
    subject: SubjectEntity?,
    current: Boolean,
    next: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    val color = subjectColor(subject)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(54.dp)) {
            BasicText(
                text = DateUtils.compactTime(entry.startMinutes),
                style = type.cardTitle.copy(color = colors.textPrimary, fontFamily = DF.type.caption.fontFamily),
            )
            BasicText(
                text = DateUtils.compactTime(entry.endMinutes),
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
            )
        }
        Box(
            Modifier
                .padding(end = 12.dp)
                .width(3.dp)
                .height(35.dp)
                .clip(CircleShape)
                .background(if (current) colors.accentInk else color.copy(alpha = 0.55f)),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = entry.title,
                    style = type.cardTitle.copy(
                        color = if (current) colors.accentInk else colors.textPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (current) {
                    Spacer(Modifier.width(6.dp))
                    StatusPill("NOW", colors.accent, colors.onAccent)
                } else if (next) {
                    Spacer(Modifier.width(6.dp))
                    StatusPill("NEXT", colors.glassQuiet, colors.textSecondary)
                }
            }
            if (entry.location.isNotBlank()) {
                BasicText(
                    text = entry.location,
                    style = type.caption.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun StatusPill(label: String, background: Color, contentColor: Color) {
    val type = DF.type
    Box(
        Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        BasicText(
            text = label,
            style = type.label.copy(color = contentColor, fontSize = 7.5.sp, letterSpacing = 0.08.sp),
        )
    }
}

@Composable
fun SubjectCard(
    subject: SubjectEntity,
    openTasks: Int,
    resources: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    val accent = subjectColor(subject)
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 8.dp),
        tint = accent.copy(alpha = if (colors.dark) 0.16f else 0.20f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(15.dp),
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth().height(136.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(39.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = subjectIcon(subject.icon),
                        contentDescription = null,
                        tint = accent.readableInk(),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                text = subject.name,
                style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 16.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = buildString {
                    append("$openTasks open")
                    if (resources > 0) append("  ·  $resources saved")
                },
                style = type.caption.copy(color = colors.textSecondary),
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
            )
        }
    }
}

fun subjectIcon(key: String): androidx.compose.ui.graphics.vector.ImageVector =
    when (key) {
        "science" -> androidx.compose.material.icons.Icons.Outlined.Science
        "math" -> androidx.compose.material.icons.Icons.Outlined.Calculate
        "language" -> androidx.compose.material.icons.Icons.Outlined.Translate
        "arts" -> androidx.compose.material.icons.Icons.Outlined.Palette
        "history" -> androidx.compose.material.icons.Icons.Outlined.AccountBalance
        "computer" -> androidx.compose.material.icons.Icons.Outlined.Computer
        "music" -> androidx.compose.material.icons.Icons.Outlined.MusicNote
        else -> androidx.compose.material.icons.Icons.Outlined.MenuBook
    }

@Composable
fun NoteRow(
    note: NoteEntity,
    subject: SubjectEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Aa",
                style = type.cardTitle.copy(
                    color = subject?.let { subjectColor(it) }?.let {
                        if (it.luminance() < 0.35f) it else colors.accentInk
                    } ?: colors.textSecondary,
                    fontSize = 13.sp,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = note.title,
                style = type.cardTitle.copy(color = colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = note.content.replace('\n', ' ').ifBlank { "Empty note" },
                style = type.caption.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        BasicText(
            text = DateUtils.relativeUpdated(note.updatedAt),
            style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun FlashcardSetRow(
    set: FlashcardSetEntity,
    subject: SubjectEntity?,
    cardCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(subject?.let { subjectColor(it).copy(alpha = 0.3f) } ?: colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = cardCount.toString(),
                style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 14.sp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = set.title,
                style = type.cardTitle.copy(color = colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "$cardCount ${if (cardCount == 1) "card" else "cards"}${subject?.let { "  ·  ${it.name}" }.orEmpty()}",
                style = type.caption.copy(color = colors.textSecondary),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun LibraryRow(
    item: LibraryItemEntity,
    subject: SubjectEntity?,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    val isUrl = item.kind == LibraryItemEntity.KIND_BOOKMARK || FileStore.isUrl(item.location)
    val missing = !isUrl && !File(item.location).exists()
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileGlyph(
            mimeType = item.mimeType,
            isBookmark = isUrl,
            tint = if (missing) colors.danger else null,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = item.title,
                style = type.cardTitle.copy(color = if (missing) colors.danger else colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = when {
                    missing -> "File is no longer available"
                    isUrl -> runCatching { java.net.URI(item.location).host?.removePrefix("www.") }
                        .getOrNull() ?: "Saved link"
                    else -> "${FileStore.humanSize(item.sizeBytes)}${subject?.let { "  ·  ${it.name}" }.orEmpty()}"
                },
                style = type.caption.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (onMore != null) {
            IconButton(
                icon = Icons.Filled.MoreHoriz,
                contentDescription = "More actions",
                onClick = onMore,
                size = 36.dp,
                glass = false,
            )
        }
    }
}

@Composable
fun CompactEmpty(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Inbox,
                null,
                tint = colors.textTertiary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(text = title, style = type.cardTitle.copy(color = colors.textPrimary))
            BasicText(
                text = message,
                style = type.caption.copy(color = colors.textSecondary),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (action != null && onAction != null) {
            BasicText(
                text = action,
                style = type.caption.copy(color = colors.accentInk, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(CircleShape)
                    .pressable(onClick = onAction)
                    .padding(8.dp),
            )
        }
    }
}

fun classStatus(
    entry: TimetableEntity,
    nowMinutes: Int = LocalTime.now().hour * 60 + LocalTime.now().minute,
): Pair<Boolean, Boolean> = when {
    nowMinutes in entry.startMinutes until entry.endMinutes -> true to false
    else -> false to false
}

fun isNextClass(
    entries: List<TimetableEntity>,
    entry: TimetableEntity,
    nowMinutes: Int = LocalTime.now().hour * 60 + LocalTime.now().minute,
): Boolean {
    val current = entries.firstOrNull { nowMinutes in it.startMinutes until it.endMinutes }
    if (current != null) return false
    val upcoming = entries.filter { it.startMinutes > nowMinutes }.minByOrNull { it.startMinutes }
    return upcoming?.id == entry.id
}

fun sortTasksForToday(tasks: List<TaskEntity>): List<TaskEntity> {
    val today = LocalDate.now().toEpochDay()
    return tasks.sortedWith(
        compareBy<TaskEntity>(
            { it.completed },
            { it.dueDate == null || it.dueDate > today },
            { it.dueMinutes ?: 24 * 60 },
            { -it.priority },
        ),
    )
}

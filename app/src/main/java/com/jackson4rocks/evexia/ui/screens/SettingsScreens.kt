package com.jackson4rocks.evexia.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.BuildConfig
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.ui.AppUiState
import com.jackson4rocks.evexia.ui.components.AppChip
import com.jackson4rocks.evexia.ui.components.AppSwitch
import com.jackson4rocks.evexia.ui.components.BottomContentPadding
import com.jackson4rocks.evexia.ui.components.ConfirmDialog
import com.jackson4rocks.evexia.ui.components.BrandMark
import com.jackson4rocks.evexia.ui.components.DividerLine
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.IconButton
import com.jackson4rocks.evexia.ui.components.JeevanAvatar
import com.jackson4rocks.evexia.ui.components.LeonAvatar
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.Radii
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.components.SegmentedControl
import com.jackson4rocks.evexia.ui.components.pressable
import com.jackson4rocks.evexia.ui.components.readableInk
import com.jackson4rocks.evexia.ui.theme.AccentChoices
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.FileStore
import com.jackson4rocks.evexia.util.ScanFilter

@Composable
fun SettingsScreen(
    state: AppUiState,
    notificationsAllowed: Boolean,
    onBack: () -> Unit,
    onRequestNotifications: () -> Unit,
    onSetTheme: (String) -> Unit,
    onSetAccent: (String) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetWeekStartsMonday: (Boolean) -> Unit,
    onSetShowWeekends: (Boolean) -> Unit,
    onSetShowCompleted: (Boolean) -> Unit,
    onSetRemindersEnabled: (Boolean) -> Unit,
    onSetReminderLead: (Int) -> Unit,
    onSetPageSize: (String) -> Unit,
    onSetScanFilter: (String) -> Unit,
    onSetPrintMonochrome: (Boolean) -> Unit,
    onClearCompletedTasks: () -> Unit,
    onClearRecent: () -> Unit,
    onOpenStudentId: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val prefs = state.preferences
    val files = state.library.filter { it.kind == LibraryItemEntity.KIND_FILE }
    val storedBytes = files.sumOf { it.sizeBytes }
    var confirmClear by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp),
    ) {
        ScreenHeader(
            title = "Settings",
            onBack = onBack,
            eyebrow = "Make Evexia yours",
        )
        Spacer(Modifier.height(8.dp))

        SettingsLabel("Appearance")
        SettingsGroup {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                SettingLine(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = "Follow the system or choose light or dark",
                )
                Spacer(Modifier.height(11.dp))
                SegmentedControl(
                    items = listOf("System", "Light", "Dark"),
                    selectedIndex = when (prefs.themeMode) {
                        "LIGHT" -> 1
                        "DARK", "AMOLED" -> 2
                        else -> 0
                    },
                    onSelected = { onSetTheme(listOf("SYSTEM", "LIGHT", "DARK")[it]) },
                )
            }
            DividerLine(Modifier.padding(horizontal = 14.dp))
            Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                SettingLine(
                    icon = Icons.Outlined.Palette,
                    title = "Accents",
                    subtitle = "Choose a colour",
                )
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    AccentChoices.forEach { choice ->
                        val selected = prefs.accent == choice.id
                        Box(
                            Modifier
                                .size(37.dp)
                                .clip(CircleShape)
                                .background(choice.fill)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) colors.textPrimary else colors.border,
                                    CircleShape,
                                )
                                .pressable { onSetAccent(choice.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = choice.label,
                                    tint = choice.fill.readableInk(),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
            DividerLine(Modifier.padding(horizontal = 14.dp))
            ToggleLine(
                icon = Icons.Outlined.Palette,
                title = "Reduce motion",
                subtitle = "Stops the glass bar, screen slides, spinners and springs",
                checked = prefs.reduceMotion,
                onCheckedChange = onSetReduceMotion,
            )
        }

        SettingsLabel("Your week")
        SettingsGroup {
            ToggleLine(
                icon = Icons.Outlined.CalendarMonth,
                title = "Week starts on Monday",
                subtitle = if (prefs.weekStartsMonday) "Monday first in the timetable" else "Sunday first in the timetable",
                checked = prefs.weekStartsMonday,
                onCheckedChange = onSetWeekStartsMonday,
            )
            DividerLine(Modifier.padding(horizontal = 14.dp))
            ToggleLine(
                icon = Icons.Outlined.CalendarMonth,
                title = "Show weekends",
                subtitle = "Keep Saturday and Sunday in the week view",
                checked = prefs.showWeekends,
                onCheckedChange = onSetShowWeekends,
            )
            DividerLine(Modifier.padding(horizontal = 14.dp))
            ToggleLine(
                icon = Icons.Outlined.CheckCircle,
                title = "Show finished tasks",
                subtitle = "Completed work stays visible in task lists",
                checked = prefs.showCompletedTasks,
                onCheckedChange = onSetShowCompleted,
            )
        }

        SettingsLabel("Reminders")
        SettingsGroup {
            ToggleLine(
                icon = Icons.Outlined.Notifications,
                title = "Task reminders",
                subtitle = "A quiet nudge before a deadline",
                checked = prefs.remindersEnabled,
                onCheckedChange = onSetRemindersEnabled,
            )
            if (prefs.remindersEnabled) {
                DividerLine(Modifier.padding(horizontal = 14.dp))
                Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    SettingLine(
                        title = "Default lead time",
                        subtitle = "Used when you add a new deadline",
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(listOf(0 to "At time", 10 to "10 min", 30 to "30 min", 60 to "1 hour", 1440 to "1 day")) { option ->
                            AppChip(
                                label = option.second,
                                selected = prefs.reminderLead == option.first,
                                onClick = { onSetReminderLead(option.first) },
                            )
                        }
                    }
                }
                if (!notificationsAllowed) {
                    DividerLine(Modifier.padding(horizontal = 14.dp))
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                "Notifications are off",
                                style = type.cardTitle.copy(color = colors.textPrimary),
                            )
                            BasicText(
                                "Allow them so reminders can reach you.",
                                style = type.caption.copy(color = colors.textSecondary),
                            )
                        }
                        SecondaryButton(
                            label = "Allow",
                            onClick = onRequestNotifications,
                            modifier = Modifier.width(82.dp),
                        )
                    }
                }
            }
        }

        SettingsLabel("Documents")
        SettingsGroup {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                SettingLine(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = "Default page size",
                    subtitle = "For scans and created PDFs",
                )
                Spacer(Modifier.height(11.dp))
                SegmentedControl(
                    items = listOf("A4", "Letter"),
                    selectedIndex = if (prefs.pdfPageSize == "LETTER") 1 else 0,
                    onSelected = { onSetPageSize(if (it == 0) "A4" else "LETTER") },
                )
                Spacer(Modifier.height(16.dp))
                SettingLine(
                    title = "Default scan finish",
                    subtitle = "Original, enhanced or crisp black & white",
                )
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    items = ScanFilter.entries.map { it.label },
                    selectedIndex = ScanFilter.entries.indexOf(ScanFilter.from(prefs.scanFilter)),
                    onSelected = { onSetScanFilter(ScanFilter.entries[it].name) },
                )
            }
            DividerLine(Modifier.padding(horizontal = 14.dp))
            ToggleLine(
                icon = Icons.Outlined.Print,
                title = "Black & white printouts",
                subtitle = "Send PDFs and images to the printer without colour",
                checked = prefs.printMonochrome,
                onCheckedChange = onSetPrintMonochrome,
            )
        }

        SettingsLabel("Storage")
        SettingsGroup {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.glassQuiet),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Storage, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        "Local storage",
                        style = type.cardTitle.copy(color = colors.textPrimary),
                    )
                    BasicText(
                        "${files.size} ${if (files.size == 1) "file" else "files"} · ${FileStore.humanSize(storedBytes)} imported",
                        style = type.caption.copy(color = colors.textSecondary),
                    )
                }
                BasicText(
                    "ON DEVICE",
                    style = type.label.copy(color = colors.textTertiary, fontSize = 7.5.sp),
                )
            }
            DividerLine(Modifier.padding(horizontal = 14.dp))
            ActionLine(
                icon = Icons.Outlined.FolderOpen,
                title = "Clear recent history",
                subtitle = "Forget which files were opened, keep the files",
                action = "Clear",
                onAction = { confirmClear = "recent" },
            )
            DividerLine(Modifier.padding(horizontal = 14.dp))
            ActionLine(
                icon = Icons.Filled.DeleteOutline,
                title = "Clear completed tasks",
                subtitle = "Permanently remove every finished task",
                action = "Clear",
                onAction = { confirmClear = "completed" },
                danger = true,
            )
        }

        SettingsLabel("More")
        SettingsGroup {
            NavLine(
                icon = Icons.Outlined.Badge,
                title = "Student ID",
                subtitle = "An optional, private digital card",
                onClick = onOpenStudentId,
            )
            DividerLine(Modifier.padding(horizontal = 14.dp))
            NavLine(
                icon = Icons.Outlined.FolderOpen,
                title = "Library",
                subtitle = "Files and saved links",
                onClick = onOpenLibrary,
            )
            DividerLine(Modifier.padding(horizontal = 14.dp))
            NavLine(
                icon = Icons.Outlined.PictureAsPdf,
                title = "PDF Studio",
                subtitle = "Write, merge and convert documents",
                onClick = onOpenConverter,
            )
            DividerLine(Modifier.padding(horizontal = 14.dp))
            NavLine(
                icon = Icons.Outlined.Info,
                title = "About Evexia",
                subtitle = "The story, the credits, the small print",
                onClick = onOpenAbout,
            )
        }

        Spacer(Modifier.height(24.dp))
        CreditsPanel()
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            BasicText(
                "Evexia ${BuildConfig.VERSION_NAME} · Android 8+ · offline by design",
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.sp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    when (confirmClear) {
        "completed" -> ConfirmDialog(
            title = "Clear completed tasks?",
            message = "Every finished task will be permanently removed. Your open tasks, notes and files are untouched.",
            confirmLabel = "Clear tasks",
            onConfirm = {
                confirmClear = null
                onClearCompletedTasks()
            },
            onDismiss = { confirmClear = null },
        )
        "recent" -> ConfirmDialog(
            title = "Clear recent history?",
            message = "Evexia will forget which files and links you last opened. The library itself is kept.",
            confirmLabel = "Clear history",
            onConfirm = {
                confirmClear = null
                onClearRecent()
            },
            onDismiss = { confirmClear = null },
        )
    }
}

@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
    ) {
        ScreenHeader(
            title = "About",
            onBack = onBack,
            eyebrow = "A quieter place for your work",
        )
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(size = 76.dp)
            Spacer(Modifier.height(14.dp))
            BasicText(
                "Evexia",
                style = type.display.copy(color = colors.textPrimary, fontSize = 34.sp),
            )
            BasicText(
                "Version ${BuildConfig.VERSION_NAME}",
                style = type.caption.copy(color = colors.textTertiary),
                modifier = Modifier.padding(top = 3.dp),
            )
            Spacer(Modifier.height(14.dp))
            BasicText(
                text = "For the student with too many tabs open and too little time.",
                style = type.serifItalic.copy(
                    color = colors.accentInk,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = "Subjects, tasks, timetables, notes and flashcards in one calm place — with a scanner and a small PDF studio when paper gets in the way.",
                style = type.bodySmall.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        Spacer(Modifier.height(24.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column {
                BasicText(
                    "WHAT IT BELIEVES",
                    style = type.label.copy(color = colors.textTertiary),
                )
                Spacer(Modifier.height(10.dp))
                AboutPoint(
                    "Local first",
                    "No Evexia account, ads or analytics. Your school data stays on this device; online Evexia AI sends prompts to Puter.",
                )
                AboutPoint("A tool, not a feed", "No streaks, badges or noise. Just the next useful thing, close at hand.")
                AboutPoint("Soft edges", "Inspired by liquid glass — layered surfaces, real depth and deliberate restraint.")
                AboutPoint("Small enough to trust", "Built with Kotlin, Compose and Room. No analytics, no tracking, no advertising.")
            }
        }
        Spacer(Modifier.height(18.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            tint = colors.glassQuiet,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Notifications,
                        null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "Notifications & widgets",
                        style = type.cardTitle.copy(color = colors.textPrimary),
                    )
                }
                Spacer(Modifier.height(7.dp))
                BasicText(
                    text = "Evexia asks for notification permission only so task reminders can arrive. Both Home screen widgets are optional: Today's Schedule and Upcoming Tasks. Add them from your launcher's widget picker and they update with your data.",
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column {
                BasicText(
                    "THANK YOU",
                    style = type.label.copy(color = colors.textTertiary),
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = "Instrument Sans and Instrument Serif are used under the SIL Open Font License. Their license files travel with the app. Material iconography is used under the Apache License 2.0.",
                    style = type.caption.copy(color = colors.textSecondary),
                )
                Spacer(Modifier.height(14.dp))
                SecondaryButton(
                    label = "Read the SIL Open Font License",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openfontlicense.org")))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        CreditsPanel()
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            BasicText(
                text = "Made slowly, for days that move quickly.",
                style = type.serifItalic.copy(color = colors.textTertiary, fontSize = 15.sp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AboutPoint(title: String, body: String) {
    val colors = DF.colors
    val type = DF.type
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(colors.accentInk),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            BasicText(title, style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.5.sp))
            BasicText(body, style = type.caption.copy(color = colors.textSecondary))
        }
    }
}

@Composable
fun CreditsPanel() {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    fun openGithub(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
        shape = Radii.card,
        strong = true,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column {
            BasicText(
                text = "PROJECT MAINTAINERS",
                style = type.label.copy(
                    color = colors.accentInk,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp,
                ),
            )
            Spacer(Modifier.height(13.dp))
            DividerLine()
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .pressable(onClick = { openGithub("https://github.com/Jackson4Rocks") })
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeonAvatar(size = 42.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "Leon Sony",
                        style = type.cardTitle.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                        ),
                    )
                    BasicText(
                        text = "Developer and Designer",
                        style = type.caption.copy(color = colors.textSecondary),
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = "github.com/Jackson4Rocks",
                        style = type.caption.copy(color = colors.accentInk),
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Open Leon's GitHub",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(13.dp))
            DividerLine()
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .pressable(onClick = { openGithub("https://github.com/JeevanDoom") })
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JeevanAvatar(size = 42.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "Ambhavaram Jeevan Kumar Reddy",
                        style = type.cardTitle.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                        ),
                    )
                    BasicText(
                        text = "Imaginer and Designer",
                        style = type.caption.copy(color = colors.textSecondary),
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = "github.com/JeevanDoom",
                        style = type.caption.copy(color = colors.accentInk),
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Open Jeevan's GitHub",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    BasicText(
        text = text.uppercase(),
        style = DF.type.label.copy(color = DF.colors.textTertiary),
        modifier = Modifier.padding(start = PagePadding + 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = PagePadding),
        contentPadding = PaddingValues(vertical = 2.dp),
        content = { Column(content = content) },
    )
}

@Composable
private fun SettingLine(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassQuiet),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(11.dp))
        }
        Column(Modifier.weight(1f)) {
            BasicText(title, style = type.cardTitle.copy(color = colors.textPrimary))
            if (subtitle != null) {
                BasicText(subtitle, style = type.caption.copy(color = colors.textSecondary))
            }
        }
    }
}

@Composable
private fun ToggleLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val type = DF.type
    val colors = DF.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.5.sp))
            BasicText(subtitle, style = type.caption.copy(color = colors.textSecondary))
        }
        Spacer(Modifier.width(8.dp))
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: String,
    onAction: () -> Unit,
    danger: Boolean = false,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (danger) colors.dangerSoft else colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (danger) colors.danger else colors.textSecondary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.5.sp))
            BasicText(subtitle, style = type.caption.copy(color = colors.textSecondary))
        }
        BasicText(
            text = action,
            style = type.caption.copy(color = if (danger) colors.danger else colors.accentInk, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .clip(CircleShape)
                .background(if (danger) colors.dangerSoft else colors.glassQuiet)
                .pressable(onClick = onAction)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun NavLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.5.sp))
            BasicText(subtitle, style = type.caption.copy(color = colors.textSecondary))
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

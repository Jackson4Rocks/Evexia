package com.jackson4rocks.evexia.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.ui.components.AmbientBackground
import com.jackson4rocks.evexia.ui.components.AppSnackbar
import com.jackson4rocks.evexia.ui.components.BrandMark
import com.jackson4rocks.evexia.ui.components.EvexiaBottomBar
import com.jackson4rocks.evexia.ui.components.LiquidGlassHost
import com.jackson4rocks.evexia.ui.components.PuterAiWebHost
import com.jackson4rocks.evexia.ui.components.ScreenTitle
import com.jackson4rocks.evexia.ui.screens.AboutScreen
import com.jackson4rocks.evexia.ui.screens.ConverterScreen
import com.jackson4rocks.evexia.ui.screens.FlashcardEditorScreen
import com.jackson4rocks.evexia.ui.screens.FlashcardsScreen
import com.jackson4rocks.evexia.ui.screens.FlowAiScreen
import com.jackson4rocks.evexia.ui.screens.HomeScreen
import com.jackson4rocks.evexia.ui.screens.LibraryScreen
import com.jackson4rocks.evexia.ui.screens.NoteEditorScreen
import com.jackson4rocks.evexia.ui.screens.NotesScreen
import com.jackson4rocks.evexia.ui.screens.ScannerScreen
import com.jackson4rocks.evexia.ui.screens.SettingsScreen
import com.jackson4rocks.evexia.ui.screens.StudentIdScreen
import com.jackson4rocks.evexia.ui.screens.StudyScreen
import com.jackson4rocks.evexia.ui.screens.SubjectDetailScreen
import com.jackson4rocks.evexia.ui.screens.SubjectEditorScreen
import com.jackson4rocks.evexia.ui.screens.SubjectsScreen
import com.jackson4rocks.evexia.ui.screens.TaskEditorScreen
import com.jackson4rocks.evexia.ui.screens.TasksScreen
import com.jackson4rocks.evexia.ui.screens.TimetableEditorScreen
import com.jackson4rocks.evexia.ui.screens.TimetableScreen
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.EvexiaTheme
import com.jackson4rocks.evexia.util.FileStore
import com.jackson4rocks.evexia.util.Printer
import java.io.File
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvexiaRoot(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav by viewModel.nav.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var snackbar by remember { mutableStateOf<String?>(null) }
    var notificationsAllowed by remember { mutableStateOf(hasNotificationPermission(context)) }
    val rootScope = rememberCoroutineScope()
    val printPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            rootScope.launch {
                val file = FileStore.copyDocumentToCache(context, uri, "print")
                if (file == null) {
                    viewModel.notify("Couldn't open that document for printing.")
                } else {
                    Printer.print(context, file, monochrome = state.preferences.printMonochrome)
                        .onSuccess { viewModel.notify("Opening print preview…") }
                        .onFailure { viewModel.notify(it.message ?: "Couldn't open the print preview.") }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = hasNotificationPermission(context)
        viewModel.notify(
            if (hasNotificationPermission(context)) "Reminders can now reach you."
            else "Notifications stay off. You can allow them in Settings.",
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar = it }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            runCatching { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                .onFailure { viewModel.notify("Notification permission can be enabled in system settings.") }
        } else {
            notificationsAllowed = true
        }
    }

    fun openLibraryItem(item: LibraryItemEntity) {
        viewModel.touchLibrary(item.id)
        if (item.kind == LibraryItemEntity.KIND_BOOKMARK || FileStore.isUrl(item.location)) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(item.location))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure { viewModel.notify("No app could open that link.") }
        } else if (!File(item.location).exists()) {
            viewModel.notify("That file is no longer available.")
        } else {
            runCatching { FileStore.open(context, item.location, item.mimeType) }
                .onFailure { viewModel.notify("No app could open that file.") }
        }
    }

    fun openLibrary(initialTab: Int = 0, subjectId: Long? = null) {
        viewModel.navigate(AppScreen.Library(initialTab, subjectId))
    }

    EvexiaTheme(preferences = state.preferences) {
        val dark = DF.colors.dark
        val reduceMotion = state.preferences.reduceMotion
        val imeVisible = WindowInsets.isImeVisible
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        BackHandler(enabled = !nav.current.isRoot) { viewModel.goBack() }

        val pageContent: @Composable () -> Unit = {
            AmbientBackground(Modifier.fillMaxSize())
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BrandMark(size = 72.dp)
                }
            } else {
                AnimatedContent(
                    targetState = nav.current,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(80)).togetherWith(fadeOut(tween(80)))
                        } else {
                            (fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 14 })
                                .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { -it / 20 })
                        }
                    },
                    label = "screen",
                    modifier = Modifier.fillMaxSize(),
                ) { screen ->
                    when (screen) {
                        AppScreen.Home -> HomeScreen(
                            state = state,
                            onSettings = { viewModel.navigate(AppScreen.Settings) },
                            onProfile = { viewModel.navigate(AppScreen.StudentId) },
                            onTasks = { viewModel.openRoot(AppScreen.Tasks) },
                            onTimetable = { viewModel.openRoot(AppScreen.Timetable) },
                            onStudy = { viewModel.navigate(AppScreen.Study) },
                            onLibrary = { openLibrary() },
                            onOpenLibraryItem = ::openLibraryItem,
                            onToggleTask = { viewModel.setTaskCompleted(it.id, !it.completed) },
                            onEditTask = { viewModel.navigate(AppScreen.TaskEditor(it)) },
                            onEditClass = { viewModel.navigate(AppScreen.TimetableEditor(it)) },
                            onAddTask = { viewModel.navigate(AppScreen.TaskEditor()) },
                            onAddSubject = { viewModel.navigate(AppScreen.SubjectEditor()) },
                            onAddNote = { viewModel.navigate(AppScreen.NoteEditor()) },
                            onAddClass = {
                                viewModel.navigate(
                                    AppScreen.TimetableEditor(dayOfWeek = LocalDate.now().dayOfWeek.value),
                                )
                            },
                            onScan = { viewModel.navigate(AppScreen.Scanner) },
                            onConvert = { viewModel.navigate(AppScreen.Converter) },
                            onPrint = { printPicker.launch(arrayOf("application/pdf", "image/*")) },
                            onFlowAi = { viewModel.openRoot(AppScreen.FlowAi) },
                        )

                        AppScreen.Subjects -> SubjectsScreen(
                            state = state,
                            onSubject = { viewModel.navigate(AppScreen.SubjectDetail(it)) },
                            onAddSubject = { viewModel.navigate(AppScreen.SubjectEditor()) },
                            onLibrary = { openLibrary() },
                            onStudy = { viewModel.navigate(AppScreen.Study) },
                        )

                        is AppScreen.SubjectDetail -> SubjectDetailScreen(
                            subjectId = screen.subjectId,
                            state = state,
                            onBack = viewModel::goBack,
                            onEditSubject = { viewModel.navigate(AppScreen.SubjectEditor(it)) },
                            onDeleteSubject = {
                                viewModel.deleteSubject(it) { viewModel.openRoot(AppScreen.Subjects) }
                            },
                            onAddTask = { viewModel.navigate(AppScreen.TaskEditor(subjectId = it)) },
                            onEditTask = { viewModel.navigate(AppScreen.TaskEditor(it)) },
                            onToggleTask = { viewModel.setTaskCompleted(it.id, !it.completed) },
                            onAddNote = { viewModel.navigate(AppScreen.NoteEditor(subjectId = it)) },
                            onOpenNote = { viewModel.navigate(AppScreen.NoteEditor(it)) },
                            onAddSet = { viewModel.navigate(AppScreen.Study) },
                            onOpenSet = { viewModel.navigate(AppScreen.Flashcards(it)) },
                            onImportFiles = { subjectId, uris -> viewModel.importFiles(uris, subjectId) },
                            onAddLink = { subjectId, title, url ->
                                viewModel.addBookmark(title, url, subjectId) {
                                    viewModel.notify("Link saved to the subject")
                                }
                            },
                            onOpenLibraryItem = ::openLibraryItem,
                            onDeleteLibraryItem = viewModel::deleteLibraryItem,
                            onLibrary = { openLibrary(subjectId = screen.subjectId) },
                            onStudy = { viewModel.navigate(AppScreen.Study) },
                        )

                        is AppScreen.SubjectEditor -> SubjectEditorScreen(
                            subjectId = screen.subjectId,
                            state = state,
                            onBack = viewModel::goBack,
                            onSave = { subject ->
                                viewModel.saveSubject(subject) { viewModel.goBack() }
                            },
                            onDelete = { id ->
                                viewModel.deleteSubject(id) { viewModel.openRoot(AppScreen.Subjects) }
                            },
                        )

                        AppScreen.Timetable -> TimetableScreen(
                            state = state,
                            onAddClass = { day ->
                                viewModel.navigate(AppScreen.TimetableEditor(dayOfWeek = day))
                            },
                            onEditClass = { viewModel.navigate(AppScreen.TimetableEditor(it)) },
                        )

                        is AppScreen.TimetableEditor -> TimetableEditorScreen(
                            state = state,
                            entryId = screen.entryId,
                            initialDay = screen.dayOfWeek,
                            onBack = viewModel::goBack,
                            onSave = { entry ->
                                viewModel.saveTimetable(entry) { viewModel.goBack() }
                            },
                            onDelete = { id ->
                                viewModel.deleteTimetable(id)
                                viewModel.goBack()
                            },
                        )

                        AppScreen.Tasks -> TasksScreen(
                            state = state,
                            onAddTask = { viewModel.navigate(AppScreen.TaskEditor()) },
                            onEditTask = { viewModel.navigate(AppScreen.TaskEditor(it)) },
                            onToggleTask = { viewModel.setTaskCompleted(it.id, !it.completed) },
                        )

                        is AppScreen.TaskEditor -> TaskEditorScreen(
                            state = state,
                            taskId = screen.taskId,
                            initialSubjectId = screen.subjectId,
                            onBack = viewModel::goBack,
                            onSave = { task ->
                                val shouldRequest = task.reminderMinutes != null &&
                                    task.dueDate != null &&
                                    !notificationsAllowed &&
                                    state.preferences.remindersEnabled
                                viewModel.saveTask(task) {
                                    viewModel.goBack()
                                    if (shouldRequest) requestNotifications()
                                }
                            },
                            onDelete = { id ->
                                viewModel.deleteTask(id)
                                viewModel.goBack()
                            },
                        )

                        AppScreen.Study -> StudyScreen(
                            state = state,
                            onBack = viewModel::goBack,
                            onAddNote = { viewModel.navigate(AppScreen.NoteEditor()) },
                            onAllNotes = { viewModel.navigate(AppScreen.Notes()) },
                            onEditNote = { viewModel.navigate(AppScreen.NoteEditor(noteId = it)) },
                            onOpenSet = { viewModel.navigate(AppScreen.Flashcards(it)) },
                            onSaveSet = { viewModel.saveFlashcardSet(it) },
                        )

                        is AppScreen.Notes -> NotesScreen(
                            state = state,
                            subjectId = screen.subjectId,
                            onBack = viewModel::goBack,
                            onAddNote = { viewModel.navigate(AppScreen.NoteEditor(subjectId = it)) },
                            onEditNote = { viewModel.navigate(AppScreen.NoteEditor(noteId = it)) },
                        )

                        is AppScreen.NoteEditor -> NoteEditorScreen(
                            state = state,
                            noteId = screen.noteId,
                            initialSubjectId = screen.subjectId,
                            onBack = viewModel::goBack,
                            onSave = { note ->
                                viewModel.saveNote(note) { viewModel.goBack() }
                            },
                            onDelete = { id ->
                                viewModel.deleteNote(id)
                                viewModel.goBack()
                            },
                        )

                        is AppScreen.Flashcards -> FlashcardsScreen(
                            state = state,
                            setId = screen.setId,
                            onBack = viewModel::goBack,
                            onAddCard = { viewModel.navigate(AppScreen.FlashcardEditor(it)) },
                            onEditCard = { setId, cardId ->
                                viewModel.navigate(AppScreen.FlashcardEditor(setId, cardId))
                            },
                            onSaveSet = { viewModel.saveFlashcardSet(it) },
                            onDeleteSet = { id ->
                                viewModel.deleteFlashcardSet(id) { viewModel.goBack() }
                            },
                        )

                        is AppScreen.FlashcardEditor -> FlashcardEditorScreen(
                            state = state,
                            setId = screen.setId,
                            cardId = screen.cardId,
                            onBack = viewModel::goBack,
                            onSave = { card ->
                                viewModel.saveFlashcard(card) { viewModel.goBack() }
                            },
                            onDelete = { id ->
                                viewModel.deleteFlashcard(id)
                                viewModel.goBack()
                            },
                        )

                        is AppScreen.Library -> LibraryScreen(
                            state = state,
                            initialTab = screen.initialTab,
                            initialSubjectId = screen.subjectId,
                            onBack = viewModel::goBack,
                            onImportFiles = { uris, subjectId -> viewModel.importFiles(uris, subjectId) },
                            onAddBookmark = { title, url, subjectId ->
                                viewModel.addBookmark(title, url, subjectId)
                            },
                            onOpenItem = ::openLibraryItem,
                            onShareItem = { item ->
                                runCatching {
                                    FileStore.share(context, item.location, item.mimeType)
                                }.onFailure { viewModel.notify("No app could share that file.") }
                            },
                            onPrintItem = { item ->
                                Printer.print(context, File(item.location), monochrome = state.preferences.printMonochrome)
                                    .onSuccess { viewModel.notify("Opening print preview…") }
                                    .onFailure {
                                        viewModel.notify(it.message ?: "Couldn't open the print preview.")
                                    }
                            },
                            onDeleteItem = viewModel::deleteLibraryItem,
                            onMoveItem = { id, subjectId -> viewModel.moveLibraryItem(id, subjectId) },
                        )

                        AppScreen.Scanner -> ScannerScreen(
                            state = state,
                            busy = busy,
                            onBack = viewModel::goBack,
                            onOpenLibrary = {
                                viewModel.openRoot(AppScreen.Home)
                                openLibrary()
                            },
                            onCreatePdf = { images, name, pageSize, filter, subjectId, onComplete ->
                                viewModel.createPdfFromImages(images, name, pageSize, filter, subjectId, onComplete)
                            },
                        )

                        AppScreen.Converter -> ConverterScreen(
                            state = state,
                            busy = busy,
                            onBack = viewModel::goBack,
                            onOpenLibrary = {
                                viewModel.openRoot(AppScreen.Home)
                                openLibrary()
                            },
                            onCreateTextPdf = { title, body, name, pageSize, subjectId, onComplete ->
                                viewModel.createPdfFromText(title, body, name, pageSize, subjectId, onComplete)
                            },
                            onCreateImagePdf = { images, name, pageSize, filter, subjectId, onComplete ->
                                viewModel.createPdfFromImages(images, name, pageSize, filter, subjectId, onComplete)
                            },
                            onMergePdfs = { files, name, subjectId, onComplete ->
                                viewModel.mergePdfs(files, name, subjectId, onComplete)
                            },
                            onPdfToImages = { file, name, onComplete ->
                                viewModel.pdfToImages(file, name, onComplete)
                            },
                            onSetPrintMonochrome = viewModel::setPrintMonochrome,
                        )

                        AppScreen.StudentId -> StudentIdScreen(
                            preferences = state.preferences,
                            subjects = state.subjects,
                            onBack = viewModel::goBack,
                            onSaveFields = viewModel::saveIdentityFields,
                            onSaveProfileImage = viewModel::saveProfileImage,
                            onSaveSchoolLogo = viewModel::saveSchoolLogo,
                        )

                        AppScreen.FlowAi -> FlowAiScreen(state = state)

                        AppScreen.Settings -> SettingsScreen(
                            state = state,
                            notificationsAllowed = notificationsAllowed,
                            onBack = viewModel::goBack,
                            onRequestNotifications = ::requestNotifications,
                            onSetTheme = viewModel::setTheme,
                            onSetAccent = viewModel::setAccent,
                            onSetReduceMotion = viewModel::setReduceMotion,
                            onSetWeekStartsMonday = viewModel::setWeekStartsMonday,
                            onSetShowWeekends = viewModel::setShowWeekends,
                            onSetShowCompleted = viewModel::setShowCompletedTasks,
                            onSetRemindersEnabled = viewModel::setRemindersEnabled,
                            onSetReminderLead = viewModel::setReminderLead,
                            onSetPageSize = viewModel::setPageSize,
                            onSetScanFilter = viewModel::setScanFilter,
                            onSetPrintMonochrome = viewModel::setPrintMonochrome,
                            onClearCompletedTasks = viewModel::clearCompletedTasks,
                            onClearRecent = viewModel::clearRecent,
                            onOpenStudentId = { viewModel.navigate(AppScreen.StudentId) },
                            onOpenLibrary = { openLibrary() },
                            onOpenConverter = { viewModel.navigate(AppScreen.Converter) },
                            onOpenAbout = { viewModel.navigate(AppScreen.About) },
                        )

                        AppScreen.About -> AboutScreen(onBack = viewModel::goBack)
                    }
                }
            }
        }
        val overlayContent: @Composable (Any?) -> Unit = { backdrop ->
            Box(Modifier.fillMaxSize()) {
                PuterAiWebHost()
                if (nav.current.isRoot && !state.loading && !imeVisible) {
                    EvexiaBottomBar(
                        selectedRoot = nav.selectedRoot,
                        onSelect = viewModel::openRoot,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    )
                }

                AppSnackbar(
                    message = snackbar,
                    onDismissed = { snackbar = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = if (nav.current.isRoot && !imeVisible) 94.dp else 16.dp),
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidGlassHost(
                enabled = nav.current.isRoot && !reduceMotion,
                content = pageContent,
                overlay = { contentBackdrop -> overlayContent(contentBackdrop) },
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                pageContent()
                overlayContent(null)
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

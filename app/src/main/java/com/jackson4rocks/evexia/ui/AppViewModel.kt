package com.jackson4rocks.evexia.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jackson4rocks.evexia.data.AppDatabase
import com.jackson4rocks.evexia.data.AppPreferences
import com.jackson4rocks.evexia.data.EvexiaRepository
import com.jackson4rocks.evexia.data.FlashcardEntity
import com.jackson4rocks.evexia.data.FlashcardSetEntity
import com.jackson4rocks.evexia.data.IdentityField
import com.jackson4rocks.evexia.data.LibraryItemEntity
import com.jackson4rocks.evexia.data.NoteEntity
import com.jackson4rocks.evexia.data.PreferencesRepository
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.data.TaskEntity
import com.jackson4rocks.evexia.data.TimetableEntity
import com.jackson4rocks.evexia.util.DocumentTools
import com.jackson4rocks.evexia.util.PdfCreationResult
import com.jackson4rocks.evexia.util.PdfPageSize
import com.jackson4rocks.evexia.util.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AppUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val timetable: List<TimetableEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val flashcardSets: List<FlashcardSetEntity> = emptyList(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val library: List<LibraryItemEntity> = emptyList(),
    val recentLibrary: List<LibraryItemEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val loading: Boolean = true,
) {
    fun subjectName(id: Long?): String? = subjects.firstOrNull { it.id == id }?.name
    fun subject(id: Long?): SubjectEntity? = subjects.firstOrNull { it.id == id }
    fun cardsFor(setId: Long): List<FlashcardEntity> = flashcards.filter { it.setId == setId }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EvexiaRepository(application, AppDatabase.get(application))
    private val preferencesRepository = PreferencesRepository(application)

    init {
        viewModelScope.launch { preferencesRepository.clearLegacyFlowAiSecrets() }
    }

    private data class CollectionData(
        val subjects: List<SubjectEntity>,
        val tasks: List<TaskEntity>,
        val timetable: List<TimetableEntity>,
        val notes: List<NoteEntity>,
        val sets: List<FlashcardSetEntity>,
    )

    private data class PersonalData(
        val cards: List<FlashcardEntity>,
        val library: List<LibraryItemEntity>,
        val recent: List<LibraryItemEntity>,
        val preferences: AppPreferences,
    )

    private val collectionFlow = combine(
        repository.subjects,
        repository.tasks,
        repository.timetable,
        repository.notes,
        repository.flashcardSets,
    ) { subjects, tasks, timetable, notes, sets ->
        CollectionData(subjects, tasks, timetable, notes, sets)
    }

    private val personalFlow = combine(
        repository.flashcards,
        repository.library,
        repository.recentLibrary,
        preferencesRepository.preferences,
    ) { cards, library, recent, preferences ->
        PersonalData(cards, library, recent, preferences)
    }

    val state: StateFlow<AppUiState> = combine(collectionFlow, personalFlow) { collection, personal ->
        AppUiState(
            subjects = collection.subjects,
            tasks = collection.tasks,
            timetable = collection.timetable,
            notes = collection.notes,
            flashcardSets = collection.sets,
            flashcards = personal.cards,
            library = personal.library,
            recentLibrary = personal.recent,
            preferences = personal.preferences,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private val _nav = MutableStateFlow(NavState())
    val nav: StateFlow<NavState> = _nav

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 12)
    val messages = _messages.asSharedFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun notify(message: String) {
        _messages.tryEmit(message)
    }

    fun navigate(screen: AppScreen) {
        _nav.update { current ->
            if (current.current == screen) current
            else current.copy(current = screen, stack = current.stack + current.current)
        }
    }

    fun goBack() {
        _nav.update { current ->
            if (current.stack.isEmpty()) {
                current.copy(current = AppScreen.Home, selectedRoot = AppScreen.Home)
            } else {
                current.copy(current = current.stack.last(), stack = current.stack.dropLast(1))
            }
        }
    }

    fun openRoot(screen: AppScreen) {
        if (!screen.isRoot) return
        _nav.update { NavState(current = screen, stack = emptyList(), selectedRoot = screen) }
    }

    fun openFromIntent(openTasks: Boolean, openTimetable: Boolean) {
        when {
            openTasks -> openRoot(AppScreen.Tasks)
            openTimetable -> openRoot(AppScreen.Timetable)
        }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                notify(t.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    fun saveSubject(subject: SubjectEntity, onSaved: (Long) -> Unit = {}) = action {
        val id = repository.saveSubject(subject)
        onSaved(id)
        notify(if (subject.id == 0L) "Subject created" else "Subject updated")
    }

    fun deleteSubject(subjectId: Long, onDeleted: () -> Unit = {}) = action {
        repository.deleteSubject(subjectId)
        onDeleted()
        notify("Subject moved to the archive")
    }

    fun saveTask(task: TaskEntity, onSaved: (Long) -> Unit = {}) = action {
        val id = repository.saveTask(task)
        onSaved(id)
        notify(if (task.id == 0L) "Task added" else "Task updated")
    }

    fun setTaskCompleted(taskId: Long, completed: Boolean) = action {
        repository.setTaskCompleted(taskId, completed)
        if (completed) notify("Nicely done. One less thing.")
    }

    fun deleteTask(taskId: Long) = action {
        repository.deleteTask(taskId)
        notify("Task deleted")
    }

    fun saveTimetable(entry: TimetableEntity, onSaved: () -> Unit = {}) = action {
        repository.saveTimetable(entry)
        onSaved()
        notify(if (entry.id == 0L) "Class added to your week" else "Class updated")
    }

    fun deleteTimetable(id: Long) = action {
        repository.deleteTimetable(id)
        notify("Class removed")
    }

    fun saveNote(note: NoteEntity, onSaved: (Long) -> Unit = {}) = action {
        val id = repository.saveNote(note)
        onSaved(id)
        notify(if (note.id == 0L) "Note saved" else "Note updated")
    }

    fun deleteNote(id: Long) = action {
        repository.deleteNote(id)
        notify("Note deleted")
    }

    fun saveFlashcardSet(set: FlashcardSetEntity, onSaved: (Long) -> Unit = {}) = action {
        val id = repository.saveFlashcardSet(set)
        onSaved(id)
    }

    fun deleteFlashcardSet(id: Long, onDeleted: () -> Unit = {}) = action {
        repository.deleteFlashcardSet(id)
        onDeleted()
        notify("Study set deleted")
    }

    fun saveFlashcard(card: FlashcardEntity, onSaved: () -> Unit = {}) = action {
        repository.saveFlashcard(card)
        onSaved()
        notify(if (card.id == 0L) "Card added" else "Card updated")
    }

    fun deleteFlashcard(id: Long) = action {
        repository.deleteFlashcard(id)
        notify("Card removed")
    }

    fun importFiles(uris: List<Uri>, subjectId: Long? = null, onDone: () -> Unit = {}) {
        if (uris.isEmpty()) return
        action {
            var imported = 0
            uris.forEach { uri ->
                if (repository.importLibraryFile(uri, subjectId) != null) imported++
            }
            onDone()
            if (imported == 0) notify("Couldn't import those files")
            else notify(if (imported == 1) "File added to your library" else "$imported files added to your library")
        }
    }

    fun addBookmark(title: String, url: String, subjectId: Long?, onDone: () -> Unit = {}) = action {
        repository.addBookmark(title, url, subjectId)
        onDone()
        notify("Link saved")
    }

    fun touchLibrary(id: Long) = action { repository.touchLibrary(id) }

    fun moveLibraryItem(id: Long, subjectId: Long?) = action {
        repository.moveLibraryItem(id, subjectId)
        notify(if (subjectId == null) "Moved out of subject" else "Moved to subject")
    }

    fun deleteLibraryItem(id: Long) = action {
        repository.deleteLibraryItem(id)
        notify("Removed from library")
    }

    fun clearRecent() = action {
        repository.clearRecentlyOpened()
        notify("Recent history cleared")
    }

    fun clearCompletedTasks() = action {
        repository.clearCompletedTasks()
        notify("Completed tasks cleared")
    }

    fun saveIdentity(field: IdentityField, value: String) = action {
        preferencesRepository.setIdentity(field, value)
    }

    fun saveIdentityFields(values: Map<IdentityField, String>) = action {
        values.forEach { (field, value) -> preferencesRepository.setIdentity(field, value) }
        notify("Student card updated")
    }

    fun saveProfileImage(path: String?) = action {
        preferencesRepository.setProfileImage(path)
        notify(if (path == null) "Profile photo removed" else "Profile photo updated")
    }

    fun saveSchoolLogo(path: String?) = action {
        preferencesRepository.setSchoolLogo(path)
        notify(if (path == null) "School logo removed" else "School logo updated")
    }

    fun setTheme(mode: String) = action { preferencesRepository.setTheme(mode) }
    fun setAccent(accent: String) = action { preferencesRepository.setAccent(accent) }
    fun setReduceMotion(value: Boolean) = action { preferencesRepository.setReduceMotion(value) }
    fun setWeekStartsMonday(value: Boolean) = action { preferencesRepository.setWeekStartsMonday(value) }
    fun setShowWeekends(value: Boolean) = action { preferencesRepository.setShowWeekends(value) }
    fun setShowCompletedTasks(value: Boolean) = action { preferencesRepository.setShowCompletedTasks(value) }
    fun setPageSize(value: String) = action { preferencesRepository.setPageSize(value) }
    fun setScanFilter(value: String) = action { preferencesRepository.setScanFilter(value) }
    fun setPrintMonochrome(value: Boolean) = action { preferencesRepository.setPrintMonochrome(value) }

    fun setRemindersEnabled(value: Boolean) = action {
        preferencesRepository.setRemindersEnabled(value)
        repository.rescheduleReminders(value)
        if (!value) notify("Reminders paused")
    }

    fun setReminderLead(value: Int) = action {
        preferencesRepository.setReminderLead(value)
        repository.rescheduleReminders(true)
    }

    fun createPdfFromImages(
        images: List<File>,
        name: String,
        pageSize: PdfPageSize,
        filter: ScanFilter,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val created = DocumentTools.imagesToPdf(
                    context = getApplication(),
                    images = images,
                    baseName = name,
                    pageSize = pageSize,
                    filter = filter,
                )
                repository.addGeneratedPdf(created.file, subjectId, name.ifBlank { created.file.name })
                created
            }
            _busy.value = false
            result.onSuccess {
                _messages.tryEmit("PDF ready · ${it.pageCount} ${if (it.pageCount == 1) "page" else "pages"}")
            }.onFailure {
                _messages.tryEmit(it.message ?: "Couldn't create the PDF")
            }
            onComplete(result)
        }
    }

    fun createPdfFromText(
        title: String,
        body: String,
        name: String,
        pageSize: PdfPageSize,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val created = DocumentTools.textToPdf(
                    context = getApplication(),
                    documentTitle = title,
                    body = body,
                    baseName = name,
                    pageSize = pageSize,
                )
                repository.addGeneratedPdf(created.file, subjectId, name.ifBlank { created.file.name })
                created
            }
            _busy.value = false
            result.onSuccess {
                _messages.tryEmit("PDF ready · ${it.pageCount} ${if (it.pageCount == 1) "page" else "pages"}")
            }.onFailure {
                _messages.tryEmit(it.message ?: "Couldn't export the PDF")
            }
            onComplete(result)
        }
    }

    fun mergePdfs(
        pdfs: List<File>,
        name: String,
        subjectId: Long?,
        onComplete: (Result<PdfCreationResult>) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val created = DocumentTools.mergePdfs(getApplication(), pdfs, name)
                repository.addGeneratedPdf(created.file, subjectId, name.ifBlank { created.file.name })
                created
            }
            _busy.value = false
            result.onSuccess {
                _messages.tryEmit("Merged PDF ready · ${it.pageCount} pages")
            }.onFailure {
                _messages.tryEmit(it.message ?: "Couldn't merge those PDFs")
            }
            onComplete(result)
        }
    }

    fun pdfToImages(
        pdf: File,
        name: String,
        onComplete: (Result<List<File>>) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) { DocumentTools.pdfToImages(getApplication(), pdf, name) }
            }
            _busy.value = false
            result.onSuccess {
                _messages.tryEmit("${it.size} page images exported")
            }.onFailure {
                _messages.tryEmit(it.message ?: "Couldn't export the pages")
            }
            onComplete(result)
        }
    }
}

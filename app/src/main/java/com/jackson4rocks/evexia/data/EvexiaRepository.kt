package com.jackson4rocks.evexia.data

import android.content.Context
import android.net.Uri
import com.jackson4rocks.evexia.notifications.ReminderScheduler
import com.jackson4rocks.evexia.util.DateUtils
import com.jackson4rocks.evexia.util.FileStore
import com.jackson4rocks.evexia.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate

class EvexiaRepository(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val subjectDao = database.subjects()
    private val taskDao = database.tasks()
    private val timetableDao = database.timetable()
    private val noteDao = database.notes()
    private val flashcardDao = database.flashcards()
    private val libraryDao = database.library()

    val subjects: Flow<List<SubjectEntity>> = subjectDao.observeAll()
    val tasks: Flow<List<TaskEntity>> = taskDao.observeAll()
    val timetable: Flow<List<TimetableEntity>> = timetableDao.observeAll()
    val notes: Flow<List<NoteEntity>> = noteDao.observeAll()
    val flashcardSets: Flow<List<FlashcardSetEntity>> = flashcardDao.observeSets()
    val flashcards: Flow<List<FlashcardEntity>> = flashcardDao.observeAllCards()
    val library: Flow<List<LibraryItemEntity>> = libraryDao.observeAll()
    val recentLibrary: Flow<List<LibraryItemEntity>> = libraryDao.observeRecent()

    fun subject(id: Long): Flow<SubjectEntity?> = subjectDao.observeById(id)
    fun subjectTasks(id: Long): Flow<List<TaskEntity>> = taskDao.observeBySubject(id)
    fun subjectNotes(id: Long): Flow<List<NoteEntity>> = noteDao.observeBySubject(id)
    fun subjectSets(id: Long): Flow<List<FlashcardSetEntity>> = flashcardDao.observeSetsBySubject(id)
    fun subjectLibrary(id: Long): Flow<List<LibraryItemEntity>> = libraryDao.observeBySubject(id)
    fun cards(setId: Long): Flow<List<FlashcardEntity>> = flashcardDao.observeCards(setId)
    fun flashcardSet(id: Long): Flow<FlashcardSetEntity?> = kotlinx.coroutines.flow.flow {
        emit(flashcardDao.getSet(id))
    }

    suspend fun subjectById(id: Long): SubjectEntity? = subjectDao.getById(id)
    suspend fun taskById(id: Long): TaskEntity? = taskDao.getById(id)
    suspend fun timetableById(id: Long): TimetableEntity? = timetableDao.getById(id)
    suspend fun noteById(id: Long): NoteEntity? = noteDao.getById(id)
    suspend fun flashcardSetById(id: Long): FlashcardSetEntity? = flashcardDao.getSet(id)
    suspend fun cardsForSet(setId: Long): List<FlashcardEntity> = flashcardDao.getCards(setId)
    suspend fun libraryItemById(id: Long): LibraryItemEntity? = libraryDao.getById(id)

    suspend fun saveSubject(subject: SubjectEntity): Long {
        val id = subjectDao.upsert(subject)
        WidgetUpdater.refresh(context)
        return if (subject.id == 0L) id else subject.id
    }

    suspend fun deleteSubject(id: Long) {
        subjectDao.deleteById(id)
        WidgetUpdater.refresh(context)
    }

    suspend fun saveTask(task: TaskEntity): Long {
        val id = taskDao.upsert(task)
        val taskId = if (task.id == 0L) id else task.id
        val saved = task.copy(id = taskId)
        if (saved.completed) {
            ReminderScheduler.cancel(context, taskId)
        } else {
            ReminderScheduler.schedule(context, saved)
        }
        WidgetUpdater.refresh(context)
        return taskId
    }

    suspend fun setTaskCompleted(id: Long, completed: Boolean): TaskEntity? {
        val task = taskDao.getById(id) ?: return null
        if (completed) {
            taskDao.markCompleted(id, System.currentTimeMillis())
            ReminderScheduler.cancel(context, id)
            WidgetUpdater.refresh(context)
            if (task.recurrence != "NONE" && task.dueDate != null) {
                val next = DateUtils.nextOccurrence(LocalDate.ofEpochDay(task.dueDate), task.recurrence)
                val clone = task.copy(
                    id = 0,
                    completed = false,
                    completedAt = null,
                    dueDate = next.toEpochDay(),
                    createdAt = System.currentTimeMillis(),
                )
                val newId = taskDao.upsert(clone)
                ReminderScheduler.schedule(context, clone.copy(id = newId))
            }
        } else {
            val reopened = task.copy(completed = false, completedAt = null)
            taskDao.upsert(reopened)
            ReminderScheduler.schedule(context, reopened)
        }
        WidgetUpdater.refresh(context)
        return taskDao.getById(id)
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
        ReminderScheduler.cancel(context, id)
        WidgetUpdater.refresh(context)
    }

    suspend fun clearCompletedTasks() {
        taskDao.deleteCompleted()
        WidgetUpdater.refresh(context)
    }

    suspend fun saveTimetable(entry: TimetableEntity): Long {
        val id = timetableDao.upsert(entry)
        WidgetUpdater.refresh(context)
        return if (entry.id == 0L) id else entry.id
    }

    suspend fun deleteTimetable(id: Long) {
        timetableDao.deleteById(id)
        WidgetUpdater.refresh(context)
    }

    suspend fun saveNote(note: NoteEntity): Long {
        val withUpdated = note.copy(updatedAt = System.currentTimeMillis())
        val id = noteDao.upsert(withUpdated)
        WidgetUpdater.refresh(context)
        return if (note.id == 0L) id else note.id
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteById(id)
        WidgetUpdater.refresh(context)
    }

    suspend fun saveFlashcardSet(set: FlashcardSetEntity): Long {
        val id = flashcardDao.upsertSet(set)
        return if (set.id == 0L) id else set.id
    }

    suspend fun deleteFlashcardSet(id: Long) {
        flashcardDao.deleteSet(id)
    }

    suspend fun saveFlashcard(card: FlashcardEntity): Long {
        val id = flashcardDao.upsertCard(card)
        return if (card.id == 0L) id else card.id
    }

    suspend fun deleteFlashcard(id: Long) {
        flashcardDao.deleteCard(id)
    }

    suspend fun importLibraryFile(uri: Uri, subjectId: Long?): Long? {
        val original = FileStore.displayName(context, uri)
        val mime = FileStore.mimeType(context, uri)
        val size = FileStore.sizeOf(context, uri)
        val file = FileStore.importDocument(context, uri) ?: return null
        val id = libraryDao.upsert(
            LibraryItemEntity(
                title = original,
                kind = LibraryItemEntity.KIND_FILE,
                location = file.absolutePath,
                mimeType = if (mime.isBlank()) "application/octet-stream" else mime,
                sizeBytes = if (size > 0) size else file.length(),
                subjectId = subjectId,
            ),
        )
        WidgetUpdater.refresh(context)
        return id
    }

    suspend fun addGeneratedPdf(file: File, subjectId: Long? = null, title: String? = null): Long {
        val id = libraryDao.upsert(
            LibraryItemEntity(
                title = title ?: file.name,
                kind = LibraryItemEntity.KIND_FILE,
                location = file.absolutePath,
                mimeType = "application/pdf",
                sizeBytes = file.length(),
                subjectId = subjectId,
            ),
        )
        WidgetUpdater.refresh(context)
        return id
    }

    suspend fun addBookmark(title: String, url: String, subjectId: Long?): Long {
        val id = libraryDao.upsert(
            LibraryItemEntity(
                title = title.ifBlank { url.hostLabel() },
                kind = LibraryItemEntity.KIND_BOOKMARK,
                location = url,
                mimeType = "text/uri-list",
                subjectId = subjectId,
            ),
        )
        WidgetUpdater.refresh(context)
        return id
    }

    suspend fun touchLibrary(id: Long) {
        libraryDao.touch(id, System.currentTimeMillis())
    }

    suspend fun moveLibraryItem(id: Long, subjectId: Long?) {
        libraryDao.moveToSubject(id, subjectId)
    }

    suspend fun deleteLibraryItem(id: Long) {
        val item = libraryDao.getById(id)
        libraryDao.deleteById(id)
        if (item != null && item.kind == LibraryItemEntity.KIND_FILE) {
            FileStore.deleteFile(item.location)
        }
        WidgetUpdater.refresh(context)
    }

    suspend fun clearRecentlyOpened() {
        libraryDao.clearHistory()
    }

    suspend fun rescheduleReminders(enabled: Boolean) {
        if (!enabled) {
            ReminderScheduler.cancelAll(context)
            return
        }
        taskDao.getPending().forEach { ReminderScheduler.schedule(context, it) }
    }

    private fun String.hostLabel(): String =
        runCatching { Uri.parse(this).host?.removePrefix("www.").orEmpty() }
            .getOrDefault("")
            .ifBlank { "Saved link" }
}

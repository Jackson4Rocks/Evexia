package com.jackson4rocks.evexia.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeById(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?

    @Upsert
    suspend fun upsert(subject: SubjectEntity): Long

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY completed ASC, dueDate ASC, dueMinutes ASC, priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY completed ASC, dueDate ASC, dueMinutes ASC")
    fun observeBySubject(subjectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY dueDate ASC, dueMinutes ASC")
    suspend fun getPending(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE completed = 1")
    suspend fun deleteCompleted()

    @Query("UPDATE tasks SET completed = 1, completedAt = :at WHERE id = :id")
    suspend fun markCompleted(id: Long, at: Long)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun observeAll(): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable WHERE id = :id")
    suspend fun getById(id: Long): TimetableEntity?

    @Query("SELECT * FROM timetable WHERE dayOfWeek = :day ORDER BY startMinutes ASC")
    suspend fun getForDay(day: Int): List<TimetableEntity>

    @Upsert
    suspend fun upsert(entry: TimetableEntity): Long

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY updatedAt DESC")
    fun observeBySubject(subjectId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_sets ORDER BY createdAt DESC")
    fun observeSets(): Flow<List<FlashcardSetEntity>>

    @Query("SELECT * FROM flashcard_sets WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun observeSetsBySubject(subjectId: Long): Flow<List<FlashcardSetEntity>>

    @Query("SELECT * FROM flashcard_sets WHERE id = :id")
    suspend fun getSet(id: Long): FlashcardSetEntity?

    @Upsert
    suspend fun upsertSet(set: FlashcardSetEntity): Long

    @Query("DELETE FROM flashcard_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM flashcards ORDER BY position ASC, id ASC")
    fun observeAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE setId = :setId ORDER BY position ASC, id ASC")
    fun observeCards(setId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE setId = :setId ORDER BY position ASC, id ASC")
    suspend fun getCards(setId: Long): List<FlashcardEntity>

    @Upsert
    suspend fun upsertCard(card: FlashcardEntity): Long

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteCard(id: Long)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library WHERE subjectId = :subjectId ORDER BY addedAt DESC")
    fun observeBySubject(subjectId: Long): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library WHERE id = :id")
    suspend fun getById(id: Long): LibraryItemEntity?

    @Query("SELECT * FROM library ORDER BY COALESCE(lastOpenedAt, addedAt) DESC LIMIT 6")
    fun observeRecent(): Flow<List<LibraryItemEntity>>

    @Upsert
    suspend fun upsert(item: LibraryItemEntity): Long

    @Query("UPDATE library SET lastOpenedAt = :at WHERE id = :id")
    suspend fun touch(id: Long, at: Long)

    @Query("UPDATE library SET subjectId = :subjectId WHERE id = :id")
    suspend fun moveToSubject(id: Long, subjectId: Long?)

    @Query("DELETE FROM library WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM library WHERE lastOpenedAt IS NOT NULL")
    suspend fun clearHistory()
}

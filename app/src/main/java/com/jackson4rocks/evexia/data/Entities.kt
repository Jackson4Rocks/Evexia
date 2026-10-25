package com.jackson4rocks.evexia.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long = 0xFFD8FF63,
    val icon: String = "book",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId"), Index("dueDate")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subjectId: Long? = null,
    val dueDate: Long? = null,
    val dueMinutes: Int? = null,
    val priority: Int = 1,
    val completed: Boolean = false,
    val recurrence: String = "NONE",
    val reminderMinutes: Int? = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

@Entity(
    tableName = "timetable",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId"), Index("dayOfWeek")],
)
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    val dayOfWeek: Int = 1,
    val startMinutes: Int = 9 * 60,
    val endMinutes: Int = 10 * 60,
    val location: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val subjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "flashcard_sets",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId")],
)
data class FlashcardSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subjectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("setId")],
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setId: Long,
    val question: String,
    val answer: String,
    val position: Int = 0,
)

@Entity(
    tableName = "library",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId")],
)
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: String = KIND_FILE,
    val location: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val subjectId: Long? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
) {
    companion object {
        const val KIND_FILE = "FILE"
        const val KIND_BOOKMARK = "BOOKMARK"
    }
}

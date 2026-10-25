package com.jackson4rocks.evexia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SubjectEntity::class,
        TaskEntity::class,
        TimetableEntity::class,
        NoteEntity::class,
        FlashcardSetEntity::class,
        FlashcardEntity::class,
        LibraryItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjects(): SubjectDao
    abstract fun tasks(): TaskDao
    abstract fun timetable(): TimetableDao
    abstract fun notes(): NoteDao
    abstract fun flashcards(): FlashcardDao
    abstract fun library(): LibraryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docflow.db",
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { instance = it }
            }

        /**
         * Version 1 is the first public schema. Future schema bumps ship a
         * dedicated migration here and are exported to app/schemas.
         */
        val ALL_MIGRATIONS: Array<Migration> = emptyArray()
    }
}

/**
 * Kept for the next schema revision: all future migrations live alongside the
 * exported Room schema so data survives updates instead of being dropped.
 */
internal fun keepForFutureMigrationV1ToV2(): Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}

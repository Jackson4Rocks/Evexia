package com.jackson4rocks.evexia.ui

sealed interface AppScreen {
    data object Home : AppScreen
    data object Subjects : AppScreen
    data object Timetable : AppScreen
    data object Tasks : AppScreen
    data object Study : AppScreen
    data class SubjectDetail(val subjectId: Long) : AppScreen
    data class TaskEditor(val taskId: Long? = null, val subjectId: Long? = null) : AppScreen
    data class SubjectEditor(val subjectId: Long? = null) : AppScreen
    data class TimetableEditor(val entryId: Long? = null, val dayOfWeek: Int = 1) : AppScreen
    data class NoteEditor(val noteId: Long? = null, val subjectId: Long? = null) : AppScreen
    data class Flashcards(val setId: Long) : AppScreen
    data class FlashcardEditor(val setId: Long, val cardId: Long? = null) : AppScreen
    data class Notes(val subjectId: Long? = null) : AppScreen
    data object Scanner : AppScreen
    data object Converter : AppScreen
    data class Library(val initialTab: Int = 0, val subjectId: Long? = null) : AppScreen
    data object StudentId : AppScreen
    data object FlowAi : AppScreen
    data object Settings : AppScreen
    data object About : AppScreen
}

val RootScreens = listOf(AppScreen.Home, AppScreen.Subjects, AppScreen.Timetable, AppScreen.Tasks, AppScreen.FlowAi)

val AppScreen.isRoot: Boolean get() = this in RootScreens

data class NavState(
    val current: AppScreen = AppScreen.Home,
    val stack: List<AppScreen> = emptyList(),
    val selectedRoot: AppScreen = AppScreen.Home,
)

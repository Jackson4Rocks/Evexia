package com.jackson4rocks.evexia.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.docFlowDataStore: DataStore<Preferences> by preferencesDataStore(name = "docflow_preferences")

enum class IdentityField {
    NAME,
    SCHOOL,
    ID_NUMBER,
    PROGRAM,
    YEAR,
    BLOOD_GROUP,
    EMERGENCY_CONTACT,
    SCHOOL_EMAIL,
}

data class AppPreferences(
    val displayName: String = "Student",
    val schoolName: String = "",
    val identityNumber: String = "",
    val program: String = "",
    val year: String = "",
    val bloodGroup: String = "",
    val emergencyContact: String = "",
    val schoolEmail: String = "",
    val profileImagePath: String? = null,
    val schoolLogoPath: String? = null,
    val themeMode: String = "SYSTEM",
    val accent: String = "LIME",
    val reduceMotion: Boolean = false,
    val weekStartsMonday: Boolean = true,
    val showWeekends: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderLead: Int = 30,
    val pdfPageSize: String = "A4",
    val scanFilter: String = "ENHANCE",
    val printMonochrome: Boolean = false,
    val showCompletedTasks: Boolean = false,
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val displayName = stringPreferencesKey("display_name")
        val school = stringPreferencesKey("school")
        val idNumber = stringPreferencesKey("id_number")
        val program = stringPreferencesKey("program")
        val year = stringPreferencesKey("year")
        val bloodGroup = stringPreferencesKey("blood_group")
        val emergency = stringPreferencesKey("emergency")
        val schoolEmail = stringPreferencesKey("school_email")
        val profilePath = stringPreferencesKey("profile_path")
        val logoPath = stringPreferencesKey("logo_path")
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val weekMonday = booleanPreferencesKey("week_monday")
        val showWeekends = booleanPreferencesKey("show_weekends")
        val reminders = booleanPreferencesKey("reminders")
        val reminderLead = intPreferencesKey("reminder_lead")
        val pageSize = stringPreferencesKey("page_size")
        val scanFilter = stringPreferencesKey("scan_filter")
        val printMonochrome = booleanPreferencesKey("print_monochrome")
        val showCompleted = booleanPreferencesKey("show_completed")
    }

    val preferences: Flow<AppPreferences> = context.docFlowDataStore.data.map { p ->
        AppPreferences(
            displayName = p[Keys.displayName] ?: "Student",
            schoolName = p[Keys.school] ?: "",
            identityNumber = p[Keys.idNumber] ?: "",
            program = p[Keys.program] ?: "",
            year = p[Keys.year] ?: "",
            bloodGroup = p[Keys.bloodGroup] ?: "",
            emergencyContact = p[Keys.emergency] ?: "",
            schoolEmail = p[Keys.schoolEmail] ?: "",
            profileImagePath = p[Keys.profilePath],
            schoolLogoPath = p[Keys.logoPath],
            themeMode = p[Keys.theme] ?: "SYSTEM",
            accent = p[Keys.accent] ?: "LIME",
            reduceMotion = p[Keys.reduceMotion] ?: false,
            weekStartsMonday = p[Keys.weekMonday] ?: true,
            showWeekends = p[Keys.showWeekends] ?: false,
            remindersEnabled = p[Keys.reminders] ?: true,
            reminderLead = p[Keys.reminderLead] ?: 30,
            pdfPageSize = p[Keys.pageSize] ?: "A4",
            scanFilter = p[Keys.scanFilter] ?: "ENHANCE",
            printMonochrome = p[Keys.printMonochrome] ?: false,
            showCompletedTasks = p[Keys.showCompleted] ?: false,
        )
    }

    suspend fun setIdentity(field: IdentityField, value: String) {
        val key = when (field) {
            IdentityField.NAME -> Keys.displayName
            IdentityField.SCHOOL -> Keys.school
            IdentityField.ID_NUMBER -> Keys.idNumber
            IdentityField.PROGRAM -> Keys.program
            IdentityField.YEAR -> Keys.year
            IdentityField.BLOOD_GROUP -> Keys.bloodGroup
            IdentityField.EMERGENCY_CONTACT -> Keys.emergency
            IdentityField.SCHOOL_EMAIL -> Keys.schoolEmail
        }
        context.docFlowDataStore.edit { it[key] = value }
    }

    suspend fun setTheme(mode: String) = context.docFlowDataStore.edit { it[Keys.theme] = mode }
    suspend fun setAccent(accent: String) = context.docFlowDataStore.edit { it[Keys.accent] = accent }
    suspend fun setReduceMotion(value: Boolean) = context.docFlowDataStore.edit { it[Keys.reduceMotion] = value }
    suspend fun setWeekStartsMonday(value: Boolean) = context.docFlowDataStore.edit { it[Keys.weekMonday] = value }
    suspend fun setShowWeekends(value: Boolean) = context.docFlowDataStore.edit { it[Keys.showWeekends] = value }
    suspend fun setRemindersEnabled(value: Boolean) = context.docFlowDataStore.edit { it[Keys.reminders] = value }
    suspend fun setReminderLead(value: Int) = context.docFlowDataStore.edit { it[Keys.reminderLead] = value }
    suspend fun setPageSize(value: String) = context.docFlowDataStore.edit { it[Keys.pageSize] = value }
    suspend fun setScanFilter(value: String) = context.docFlowDataStore.edit { it[Keys.scanFilter] = value }
    suspend fun setPrintMonochrome(value: Boolean) = context.docFlowDataStore.edit { it[Keys.printMonochrome] = value }
    suspend fun setShowCompletedTasks(value: Boolean) = context.docFlowDataStore.edit { it[Keys.showCompleted] = value }

    /** Removes secrets left behind by the old bring-your-own-Gemini build. */
    suspend fun clearLegacyFlowAiSecrets() = context.docFlowDataStore.edit { preferences ->
        val key = stringPreferencesKey("flow_ai_api_key")
        val model = stringPreferencesKey("flow_ai_model")
        if (key in preferences || model in preferences) {
            preferences.remove(key)
            preferences.remove(model)
        }
    }
    suspend fun setProfileImage(path: String?) = context.docFlowDataStore.edit {
        if (path == null) it.remove(Keys.profilePath) else it[Keys.profilePath] = path
    }
    suspend fun setSchoolLogo(path: String?) = context.docFlowDataStore.edit {
        if (path == null) it.remove(Keys.logoPath) else it[Keys.logoPath] = path
    }
}

package com.jackson4rocks.evexia.util

import android.content.Context
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now()

    fun greeting(): String = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    fun longDate(date: LocalDate = LocalDate.now()): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

    fun shortWeekday(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun weekLabel(date: LocalDate = LocalDate.now()): String {
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        return "${monday.dayOfMonth} – ${sunday.dayOfMonth} ${sunday.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
    }

    fun timeLabel(context: Context, minutes: Int): String {
        val time = LocalTime.of((minutes / 60).coerceIn(0, 23), (minutes % 60).coerceIn(0, 59))
        return if (DateFormat.is24HourFormat(context)) {
            time.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        }
    }

    fun compactTime(minutes: Int): String =
        "%02d:%02d".format(Locale.US, (minutes / 60) % 24, minutes % 60)

    fun minutesFromTime(hour: Int, minute: Int): Int = hour * 60 + minute

    fun dueLocalDateTime(dueDateEpochDay: Long, dueMinutes: Int?): LocalDateTime {
        val date = LocalDate.ofEpochDay(dueDateEpochDay)
        val time = dueMinutes?.let { LocalTime.of(it / 60, it % 60) } ?: LocalTime.of(9, 0)
        return LocalDateTime.of(date, time)
    }

    fun dueAtMillis(dueDateEpochDay: Long, dueMinutes: Int?): Long =
        dueLocalDateTime(dueDateEpochDay, dueMinutes).atZone(zone).toInstant().toEpochMilli()

    fun dueDateFromMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun formatDue(context: Context, dueDateEpochDay: Long, dueMinutes: Int?): String {
        val date = LocalDate.ofEpochDay(dueDateEpochDay)
        val today = LocalDate.now()
        val dayText = when {
            date == today -> "Today"
            date == today.plusDays(1) -> "Tomorrow"
            date == today.minusDays(1) -> "Yesterday"
            date.isBefore(today) && ChronoUnit.DAYS.between(date, today) < 7 ->
                "${ChronoUnit.DAYS.between(date, today)} days ago"
            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
            else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }
        if (dueMinutes == null) return dayText
        return "$dayText · ${timeLabel(context, dueMinutes)}"
    }

    fun isOverdue(dueDateEpochDay: Long?, dueMinutes: Int?, completed: Boolean): Boolean {
        if (completed || dueDateEpochDay == null) return false
        val date = LocalDate.ofEpochDay(dueDateEpochDay)
        // A date without a time means "by the end of that day", not 9am.
        if (dueMinutes == null) return date.isBefore(LocalDate.now())
        return dueLocalDateTime(dueDateEpochDay, dueMinutes).isBefore(LocalDateTime.now())
    }

    fun dueSoon(dueDateEpochDay: Long?): Boolean {
        if (dueDateEpochDay == null) return false
        return dueDateEpochDay <= LocalDate.now().plusDays(2).toEpochDay()
    }

    fun nextOccurrence(date: LocalDate, recurrence: String): LocalDate = when (recurrence) {
        "DAILY" -> date.plusDays(1)
        "WEEKLY" -> date.plusWeeks(1)
        "MONTHLY" -> date.plusMonths(1)
        else -> date
    }

    fun recurrenceLabel(value: String): String = when (value) {
        "DAILY" -> "Every day"
        "WEEKLY" -> "Every week"
        "MONTHLY" -> "Every month"
        else -> "One time"
    }

    fun dayName(day: Int): String =
        DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Locale.getDefault())

    fun dayInitial(day: Int): String =
        DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(TextStyle.NARROW, Locale.getDefault()).uppercase()

    fun orderedDays(weekStartsMonday: Boolean): List<Int> =
        if (weekStartsMonday) listOf(1, 2, 3, 4, 5, 6, 7) else listOf(7, 1, 2, 3, 4, 5, 6)

    fun isToday(dayOfWeek: Int): Boolean = LocalDate.now().dayOfWeek.value == dayOfWeek

    fun relativeUpdated(millis: Long): String {
        val minutes = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(millis), Instant.now())
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d ago"
            else -> dueDateFromMillis(millis).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
        }
    }
}

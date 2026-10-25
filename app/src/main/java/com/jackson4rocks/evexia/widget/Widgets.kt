package com.jackson4rocks.evexia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.jackson4rocks.evexia.MainActivity
import com.jackson4rocks.evexia.R
import com.jackson4rocks.evexia.data.AppDatabase
import com.jackson4rocks.evexia.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object WidgetUpdater {
    fun refresh(context: Context) {
        listOf(TodayScheduleWidgetProvider::class.java, UpcomingTasksWidgetProvider::class.java).forEach { type ->
            val component = ComponentName(context, type)
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(component)
            if (ids.isEmpty()) return@forEach
            val intent = Intent(context, type).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}

class TodayScheduleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entries = AppDatabase.get(context).timetable().getForDay(LocalDate.now().dayOfWeek.value)
                val views = RemoteViews(context.packageName, R.layout.widget_today)
                appWidgetIds.forEach { id ->
                    val date = LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()),
                    )
                    views.setTextViewText(R.id.widget_date, date)
                    val rowIds = intArrayOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3)
                    for (i in rowIds.indices) {
                        val entry = entries.getOrNull(i)
                        if (entry == null) {
                            views.setViewVisibility(rowIds[i], View.GONE)
                        } else {
                            views.setViewVisibility(rowIds[i], View.VISIBLE)
                            views.setTextViewText(
                                rowIds[i],
                                "${DateUtils.compactTime(entry.startMinutes)}  ${entry.title}${entry.location.takeIf { it.isNotBlank() }?.let { "  ·  $it" }.orEmpty()}",
                            )
                        }
                    }
                    if (entries.isEmpty()) {
                        views.setViewVisibility(R.id.widget_row_1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_row_1, "No classes today. A little room to breathe.")
                        views.setViewVisibility(R.id.widget_row_2, View.GONE)
                        views.setViewVisibility(R.id.widget_row_3, View.GONE)
                    }
                    views.setOnClickPendingIntent(R.id.widget_today_root, launchIntent(context, openTimetable = true))
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class UpcomingTasksWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val tasks = AppDatabase.get(context).tasks().getPending()
                    .filter { it.dueDate == null || it.dueDate >= LocalDate.now().minusDays(1).toEpochDay() }
                    .take(4)
                val views = RemoteViews(context.packageName, R.layout.widget_tasks)
                appWidgetIds.forEach { id ->
                    val rowIds = intArrayOf(R.id.widget_task_1, R.id.widget_task_2, R.id.widget_task_3, R.id.widget_task_4)
                    for (i in rowIds.indices) {
                        val task = tasks.getOrNull(i)
                        if (task == null) {
                            views.setViewVisibility(rowIds[i], View.GONE)
                        } else {
                            views.setViewVisibility(rowIds[i], View.VISIBLE)
                            val due = task.dueDate?.let { DateUtils.formatDue(context, it, task.dueMinutes) }
                                ?: "No due date"
                            views.setTextViewText(rowIds[i], "${task.title}\n$due")
                        }
                    }
                    if (tasks.isEmpty()) {
                        views.setViewVisibility(R.id.widget_task_1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_1, "You're all clear.\nNothing waiting on you.")
                        views.setViewVisibility(R.id.widget_task_2, View.GONE)
                        views.setViewVisibility(R.id.widget_task_3, View.GONE)
                        views.setViewVisibility(R.id.widget_task_4, View.GONE)
                    }
                    views.setTextViewText(
                        R.id.widget_task_count,
                        if (tasks.isEmpty()) "ALL CLEAR" else "${tasks.size} OPEN",
                    )
                    views.setOnClickPendingIntent(R.id.widget_tasks_root, launchIntent(context, openTasks = true))
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

private fun launchIntent(context: Context, openTimetable: Boolean = false, openTasks: Boolean = false): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (openTimetable) putExtra("open_timetable", true)
        if (openTasks) putExtra("open_tasks", true)
    }
    return PendingIntent.getActivity(
        context,
        if (openTimetable) 91 else 92,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

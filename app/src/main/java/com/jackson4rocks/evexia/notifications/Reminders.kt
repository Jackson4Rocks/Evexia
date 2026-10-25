package com.jackson4rocks.evexia.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jackson4rocks.evexia.MainActivity
import com.jackson4rocks.evexia.R
import com.jackson4rocks.evexia.data.AppDatabase
import com.jackson4rocks.evexia.data.AppPreferences
import com.jackson4rocks.evexia.data.TaskEntity
import com.jackson4rocks.evexia.util.DateUtils
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object Notifications {
    const val CHANNEL_TASKS = "task_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = android.app.NotificationChannel(
            CHANNEL_TASKS,
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Gentle nudges before assignments and deadlines"
            enableLights(false)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

    @SuppressLint("MissingPermission")
    fun showTaskReminder(context: Context, task: TaskEntity) {
        if (!canPost(context)) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tasks", true)
        }
        val pending = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val due = if (task.dueDate != null) {
            DateUtils.formatDue(context, task.dueDate, task.dueMinutes)
        } else {
            "A deadline is coming up"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title.take(80))
            .setContentText(due)
            .setStyle(NotificationCompat.BigTextStyle().bigText(due))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(task.id.toInt() + 4200, notification)
        }
    }
}

object ReminderScheduler {
    private const val TAG = "docflow_task_reminder"
    private fun workName(taskId: Long) = "docflow_task_reminder_$taskId"

    fun schedule(context: Context, task: TaskEntity) {
        val lead = task.reminderMinutes
        val dueDate = task.dueDate
        if (task.completed || lead == null || dueDate == null) {
            cancel(context, task.id)
            return
        }
        val due = DateUtils.dueAtMillis(dueDate, task.dueMinutes)
        val notifyAt = due - lead * 60_000L
        val delay = notifyAt - System.currentTimeMillis()
        if (delay <= 0L) {
            cancel(context, task.id)
            return
        }
        val data = Data.Builder()
            .putLong(TaskReminderWorker.KEY_TASK_ID, task.id)
            .build()
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(task.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(taskId))
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }
}

class TaskReminderWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters,
) : androidx.work.CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (taskId <= 0L) return Result.success()
        val prefs = runCatching {
            PreferencesReader.read(applicationContext)
        }.getOrNull()
        if (prefs?.remindersEnabled == false) return Result.success()
        val task = AppDatabase.get(applicationContext).tasks().getById(taskId)
            ?: return Result.success()
        if (task.completed) return Result.success()
        Notifications.showTaskReminder(applicationContext, task)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}

private object PreferencesReader {
    suspend fun read(context: Context): AppPreferences =
        com.jackson4rocks.evexia.data.PreferencesRepository(context).preferences.first()
}

package com.example.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.CalendarEvent
import com.example.data.FamilyTask
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderScheduler {
    private const val DEFAULT_REMINDER_TIME = "09:00"
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun triggerTimeMillis(date: String, time: String): Long? {
        return try {
            dateTimeFormat.parse("$date $time")?.time
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun schedule(context: Context, requestCode: Int, uri: Uri, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply { data = uri }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancel(context: Context, requestCode: Int, uri: Uri) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply { data = uri }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun calendarEventUri(eventId: Int): Uri = Uri.parse("reminder://calendarEvent/$eventId")
    private fun taskUri(taskId: Int): Uri = Uri.parse("reminder://task/$taskId")

    fun scheduleCalendarEventReminder(context: Context, event: CalendarEvent) {
        val time = if (event.isAllDay) DEFAULT_REMINDER_TIME else event.time
        val triggerAtMillis = triggerTimeMillis(event.date, time) ?: return
        schedule(context, event.id, calendarEventUri(event.id), triggerAtMillis)
    }

    fun cancelCalendarEventReminder(context: Context, eventId: Int) {
        cancel(context, eventId, calendarEventUri(eventId))
    }

    fun scheduleTaskReminder(context: Context, task: FamilyTask) {
        if (task.dueDate.isBlank()) return
        val triggerAtMillis = triggerTimeMillis(task.dueDate, DEFAULT_REMINDER_TIME) ?: return
        schedule(context, task.id, taskUri(task.id), triggerAtMillis)
    }

    fun cancelTaskReminder(context: Context, taskId: Int) {
        cancel(context, taskId, taskUri(taskId))
    }

    /** Re-registers every pending reminder from Room. Used after device reboot. */
    suspend fun rescheduleAll(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val events = db.calendarDao().getAllEvents().first()
        val tasks = db.taskDao().getAllTasks().first()

        for (event in events) {
            if (!event.isCompleted) scheduleCalendarEventReminder(context, event)
        }
        for (task in tasks) {
            if (!task.isCompleted) scheduleTaskReminder(context, task)
        }
    }
}

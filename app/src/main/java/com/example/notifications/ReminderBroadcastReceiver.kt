package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val id = uri.lastPathSegment?.toIntOrNull() ?: return
        val type = uri.host ?: return
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                when (type) {
                    "calendarEvent" -> {
                        val event = db.calendarDao().getEventById(id)
                        if (event != null && !event.isCompleted) {
                            val whenLabel = if (event.isAllDay) event.date else "${event.date} ${event.time}"
                            NotificationHelper.showCalendarEventReminderNotification(appContext, event.id, event.title, whenLabel)
                        }
                    }
                    "task" -> {
                        val task = db.taskDao().getTaskById(id)
                        if (task != null && !task.isCompleted) {
                            NotificationHelper.showTaskReminderNotification(appContext, task.id, task.title, task.dueDate)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

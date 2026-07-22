package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_CHAT_ID = "chat_messages"
    const val CHANNEL_GOALS_ID = "goal_reached"
    const val CHANNEL_POLLS_ID = "new_polls"
    const val CHANNEL_CALENDAR_REMINDERS_ID = "calendar_event_reminders"
    const val CHANNEL_TASK_REMINDERS_ID = "task_reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val chatChannel = NotificationChannel(
            CHANNEL_CHAT_ID,
            "Family Chat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New messages in the family chat"
        }

        val goalsChannel = NotificationChannel(
            CHANNEL_GOALS_ID,
            "Goals Reached",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when a savings goal is fully funded"
        }

        val pollsChannel = NotificationChannel(
            CHANNEL_POLLS_ID,
            "Family Polls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New polls created by family members"
        }

        val calendarRemindersChannel = NotificationChannel(
            CHANNEL_CALENDAR_REMINDERS_ID,
            "Calendar Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming family calendar events"
        }

        val taskRemindersChannel = NotificationChannel(
            CHANNEL_TASK_REMINDERS_ID,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for tasks due today"
        }

        manager.createNotificationChannel(chatChannel)
        manager.createNotificationChannel(goalsChannel)
        manager.createNotificationChannel(pollsChannel)
        manager.createNotificationChannel(calendarRemindersChannel)
        manager.createNotificationChannel(taskRemindersChannel)
    }

    fun showChatMessageNotification(context: Context, senderName: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context))
            .build()

        notify(context, senderName.hashCode(), notification)
    }

    fun showGoalReachedNotification(context: Context, memberName: String, goalTitle: String) {
        val title = "Goal reached!"
        val text = "$memberName just fully funded \"$goalTitle\""

        val notification = NotificationCompat.Builder(context, CHANNEL_GOALS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context))
            .build()

        notify(context, goalTitle.hashCode(), notification)
    }

    fun showNewPollNotification(context: Context, creatorName: String, question: String) {
        val title = "New poll from $creatorName"

        val notification = NotificationCompat.Builder(context, CHANNEL_POLLS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(question)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question))
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context))
            .build()

        notify(context, question.hashCode(), notification)
    }

    fun showCalendarEventReminderNotification(context: Context, eventId: Int, title: String, whenLabel: String) {
        val text = "$title - $whenLabel"

        val notification = NotificationCompat.Builder(context, CHANNEL_CALENDAR_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Upcoming event")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context))
            .build()

        notify(context, "calendar_event_$eventId".hashCode(), notification)
    }

    fun showTaskReminderNotification(context: Context, taskId: Int, title: String, dueDateLabel: String) {
        val text = "$title - due $dueDateLabel"

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Task reminder")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(buildOpenAppIntent(context))
            .build()

        notify(context, "task_$taskId".hashCode(), notification)
    }

    private fun buildOpenAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check above and this call; ignore.
        }
    }
}

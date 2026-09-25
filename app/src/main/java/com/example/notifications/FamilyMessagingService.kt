package com.example.notifications

import com.example.data.PushTokenService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pushes sent by the Cloud Functions in functions/. When the app is in the background
 * or killed, the system shows the notification payload itself; this service only runs for
 * foreground delivery, where we show it through NotificationHelper instead.
 */
class FamilyMessagingService : FirebaseMessagingService() {

    companion object {
        // Set by ChatViewModel so chat pushes are suppressed while the chat is on screen.
        @Volatile
        var chatScreenVisible: Boolean = false
    }

    override fun onNewToken(token: String) {
        PushTokenService.onTokenRefreshed(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        NotificationHelper.createChannels(this)

        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: return
        val body = message.notification?.body ?: data["body"] ?: ""

        when (data["type"]) {
            "chat" -> if (!chatScreenVisible) {
                NotificationHelper.showChatMessageNotification(this, title, body)
            }
            "poll" -> NotificationHelper.showPushNotification(this, NotificationHelper.CHANNEL_POLLS_ID, title, body, data["id"])
            "goal" -> NotificationHelper.showPushNotification(this, NotificationHelper.CHANNEL_GOALS_ID, title, body, data["id"])
            "task" -> NotificationHelper.showPushNotification(this, NotificationHelper.CHANNEL_NEW_TASKS_ID, title, body, data["id"])
            "wallet" -> NotificationHelper.showPushNotification(this, NotificationHelper.CHANNEL_WALLET_ID, title, body, data["id"])
        }
    }
}

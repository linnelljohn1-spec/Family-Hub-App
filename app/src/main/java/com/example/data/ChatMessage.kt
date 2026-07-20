package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val clientMessageId: String,
    val senderMemberId: Int,
    val senderName: String,
    val senderAvatarColorHex: String,
    val text: String,
    val timestamp: Long,
    val syncGroupCode: String
)

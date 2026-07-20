package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    fun getMessagesForGroup(syncGroupCode: String): Flow<List<ChatMessage>> =
        chatDao.getMessagesForGroup(syncGroupCode)

    suspend fun insertMessage(message: ChatMessage) = chatDao.insertMessage(message)

    suspend fun insertMessages(messages: List<ChatMessage>) = chatDao.insertMessages(messages)
}

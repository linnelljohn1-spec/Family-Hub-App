package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.notifications.NotificationHelper
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatRepository = ChatRepository(AppDatabase.getDatabase(application).chatDao())
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

    private val _syncGroupCode = MutableStateFlow("")
    private val _activeMemberId = MutableStateFlow(-1)
    private var activeMemberName: String = ""
    private var activeMemberColorHex: String = "#6750A4"

    private val _isChatScreenVisible = MutableStateFlow(false)
    private val _lastViewedTimestamp = MutableStateFlow(0L)

    private var messagesListener: ListenerRegistration? = null
    private var goalEventsListener: ListenerRegistration? = null

    val messages: StateFlow<List<ChatMessage>> = _syncGroupCode
        .flatMapLatest { code ->
            if (code.isEmpty()) flowOf(emptyList()) else chatRepository.getMessagesForGroup(code)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = combine(messages, _lastViewedTimestamp, _activeMemberId) { msgs, lastViewed, activeId ->
        msgs.count { it.timestamp > lastViewed && it.senderMemberId != activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSyncGroupCode(code: String) {
        if (_syncGroupCode.value == code) return
        _syncGroupCode.value = code
        _lastViewedTimestamp.value = prefs.getLong(lastViewedKey(code), 0L)
        registerListeners(code)
    }

    fun setActiveMember(memberId: Int, name: String, avatarColorHex: String) {
        _activeMemberId.value = memberId
        activeMemberName = name
        activeMemberColorHex = avatarColorHex
    }

    fun setChatScreenVisible(visible: Boolean) {
        _isChatScreenVisible.value = visible
        if (visible) markAllRead()
    }

    fun markAllRead() {
        val code = _syncGroupCode.value
        val now = System.currentTimeMillis()
        _lastViewedTimestamp.value = now
        prefs.edit().putLong(lastViewedKey(code), now).apply()
    }

    fun sendMessage(text: String) {
        val code = _syncGroupCode.value
        val memberId = _activeMemberId.value
        if (code.isBlank() || text.isBlank() || memberId == -1) return

        val message = hashMapOf(
            "clientMessageId" to UUID.randomUUID().toString(),
            "senderMemberId" to memberId,
            "senderName" to activeMemberName,
            "senderAvatarColorHex" to activeMemberColorHex,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("families")
            .document(code)
            .collection("messages")
            .add(message)
    }

    private fun registerListeners(code: String) {
        messagesListener?.remove()
        goalEventsListener?.remove()
        if (code.isBlank()) return

        val familyDoc = firestore.collection("families").document(code)
        var isFirstMessageSnapshot = true
        var isFirstGoalEventSnapshot = true

        messagesListener = familyDoc.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val shouldNotify = !isFirstMessageSnapshot
                isFirstMessageSnapshot = false

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        if (change.type != DocumentChange.Type.ADDED) continue
                        val data = change.document.data
                        val chatMessage = ChatMessage(
                            clientMessageId = data["clientMessageId"] as? String ?: change.document.id,
                            senderMemberId = (data["senderMemberId"] as? Long)?.toInt() ?: -1,
                            senderName = data["senderName"] as? String ?: "",
                            senderAvatarColorHex = data["senderAvatarColorHex"] as? String ?: "#6750A4",
                            text = data["text"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Long) ?: 0L,
                            syncGroupCode = code
                        )
                        chatRepository.insertMessage(chatMessage)

                        if (shouldNotify && chatMessage.senderMemberId != _activeMemberId.value && !_isChatScreenVisible.value) {
                            NotificationHelper.showChatMessageNotification(
                                getApplication(),
                                chatMessage.senderName,
                                chatMessage.text
                            )
                        }
                    }
                }
            }

        goalEventsListener = familyDoc.collection("goalEvents")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val shouldNotify = !isFirstGoalEventSnapshot
                isFirstGoalEventSnapshot = false

                for (change in snapshot.documentChanges) {
                    if (change.type != DocumentChange.Type.ADDED) continue
                    if (!shouldNotify) continue
                    val data = change.document.data
                    val memberId = (data["memberId"] as? Long)?.toInt() ?: -1
                    if (memberId == _activeMemberId.value) continue
                    val memberName = data["memberName"] as? String ?: "A family member"
                    val goalTitle = data["goalTitle"] as? String ?: "a goal"
                    NotificationHelper.showGoalReachedNotification(getApplication(), memberName, goalTitle)
                }
            }
    }

    private fun lastViewedKey(code: String) = "last_viewed_ts_$code"

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        goalEventsListener?.remove()
    }
}

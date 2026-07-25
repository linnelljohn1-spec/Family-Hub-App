package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Poll
import com.example.data.PollMode
import com.example.data.PollOption
import com.example.data.PollRepository
import com.example.data.PollVote
import com.example.notifications.NotificationHelper
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PollsViewModel(application: Application) : AndroidViewModel(application) {
    private val pollRepository = PollRepository(AppDatabase.getDatabase(application).pollDao())
    private val firestore = FirebaseFirestore.getInstance()

    private val _syncGroupCode = MutableStateFlow("")
    private val _activeMemberId = MutableStateFlow(-1)
    private val _isActiveAdmin = MutableStateFlow(false)
    private var activeMemberName: String = ""

    private var pollsListener: ListenerRegistration? = null
    private val voteListeners = mutableMapOf<String, ListenerRegistration>()
    private val optionListeners = mutableMapOf<String, ListenerRegistration>()

    val polls: StateFlow<List<Poll>> = pollRepository.allPolls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val votes: StateFlow<List<PollVote>> = pollRepository.allVotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pollOptions: StateFlow<List<PollOption>> = pollRepository.allOptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openUnvotedPollsCount: StateFlow<Int> = combine(polls, votes, _activeMemberId) { pollList, voteList, memberId ->
        pollList.count { poll ->
            when (poll.mode) {
                PollMode.SPIN -> poll.spinResultIndex == null
                else -> !poll.isClosed && voteList.none { it.pollId == poll.id && it.memberId == memberId }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSyncGroupCode(code: String) {
        if (_syncGroupCode.value == code) return
        _syncGroupCode.value = code
        registerListeners(code)
    }

    fun setActiveMember(memberId: Int, name: String, isAdmin: Boolean) {
        _activeMemberId.value = memberId
        activeMemberName = name
        _isActiveAdmin.value = isAdmin
    }

    fun createPoll(question: String, options: List<String>, mode: String = PollMode.VOTE) {
        val code = _syncGroupCode.value
        val memberId = _activeMemberId.value
        if (code.isBlank() || memberId == -1) return

        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank()) return
        val cleanOptions = if (mode == PollMode.SPIN) {
            emptyList()
        } else {
            options.map { it.trim() }.filter { it.isNotBlank() }
        }
        if (mode != PollMode.SPIN && (cleanOptions.size < 2 || cleanOptions.size > 6)) return

        val data = hashMapOf(
            "question" to cleanQuestion,
            "options" to cleanOptions,
            "createdByMemberId" to memberId,
            "createdByName" to activeMemberName,
            "createdAt" to System.currentTimeMillis(),
            "isClosed" to false,
            "mode" to mode
        )

        firestore.collection("families")
            .document(code)
            .collection("polls")
            .add(data)
    }

    fun castVote(pollId: Int, optionIndex: Int) {
        val code = _syncGroupCode.value
        val memberId = _activeMemberId.value
        if (code.isBlank() || memberId == -1) return
        val poll = polls.value.find { it.id == pollId } ?: return
        val firestoreId = poll.firestoreId ?: return
        if (poll.isClosed) return

        val data = hashMapOf(
            "optionIndex" to optionIndex,
            "votedAt" to System.currentTimeMillis()
        )

        firestore.collection("families")
            .document(code)
            .collection("polls")
            .document(firestoreId)
            .collection("votes")
            .document(memberId.toString())
            .set(data)
    }

    fun addOption(poll: Poll, text: String) {
        val code = _syncGroupCode.value
        val memberId = _activeMemberId.value
        val firestoreId = poll.firestoreId ?: return
        if (code.isBlank() || memberId == -1 || poll.spinResultIndex != null) return

        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val existing = pollOptions.value.filter { it.pollId == poll.id }
        if (existing.size >= 12) return
        if (existing.any { it.text.equals(cleanText, ignoreCase = true) }) return

        val data = hashMapOf(
            "text" to cleanText,
            "createdByMemberId" to memberId,
            "createdByName" to activeMemberName,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("families")
            .document(code)
            .collection("polls")
            .document(firestoreId)
            .collection("options")
            .add(data)
    }

    fun deleteOption(poll: Poll, option: PollOption) {
        val code = _syncGroupCode.value
        val firestoreId = poll.firestoreId ?: return
        val optionFirestoreId = option.firestoreId ?: return
        if (code.isBlank() || poll.spinResultIndex != null) return
        if (!(canModify(poll) || option.createdByMemberId == _activeMemberId.value)) return

        firestore.collection("families")
            .document(code)
            .collection("polls")
            .document(firestoreId)
            .collection("options")
            .document(optionFirestoreId)
            .delete()
    }

    fun spinWheel(poll: Poll) {
        if (!canModify(poll)) return
        val code = _syncGroupCode.value
        val firestoreId = poll.firestoreId ?: return
        if (code.isBlank() || poll.spinResultIndex != null) return

        val pollOptionsForPoll = pollOptions.value.filter { it.pollId == poll.id }.sortedBy { it.createdAt }
        if (pollOptionsForPoll.size < 2) return
        val winningIndex = pollOptionsForPoll.indices.random()

        val pollDoc = firestore.collection("families")
            .document(code)
            .collection("polls")
            .document(firestoreId)

        firestore.runTransaction { txn ->
            val snapshot = txn.get(pollDoc)
            if (snapshot.getLong("spinResultIndex") == null) {
                txn.update(
                    pollDoc,
                    mapOf(
                        "spinResultIndex" to winningIndex,
                        "spinStartedAt" to System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun closePoll(poll: Poll) {
        if (!canModify(poll)) return
        val code = _syncGroupCode.value
        val firestoreId = poll.firestoreId ?: return
        if (code.isBlank()) return

        firestore.collection("families")
            .document(code)
            .collection("polls")
            .document(firestoreId)
            .update("isClosed", true)
    }

    fun deletePoll(poll: Poll) {
        if (!canModify(poll)) return
        val code = _syncGroupCode.value
        val firestoreId = poll.firestoreId ?: return
        if (code.isBlank()) return

        val pollDoc = firestore.collection("families")
            .document(code)
            .collection("polls")
            .document(firestoreId)

        pollDoc.collection("votes").get().addOnSuccessListener { snapshot ->
            val batch = firestore.batch()
            for (voteDoc in snapshot.documents) {
                batch.delete(voteDoc.reference)
            }
            batch.delete(pollDoc)
            batch.commit()
        }
    }

    fun canModify(poll: Poll): Boolean = _isActiveAdmin.value || _activeMemberId.value == poll.createdByMemberId

    private fun registerListeners(code: String) {
        pollsListener?.remove()
        voteListeners.values.forEach { it.remove() }
        voteListeners.clear()
        optionListeners.values.forEach { it.remove() }
        optionListeners.clear()
        if (code.isBlank()) return

        val familyDoc = firestore.collection("families").document(code)
        var isFirstPollSnapshot = true

        pollsListener = familyDoc.collection("polls")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val shouldNotify = !isFirstPollSnapshot
                isFirstPollSnapshot = false

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val existing = pollRepository.getPollByFirestoreId(doc.id)
                                val createdByMemberId = (data["createdByMemberId"] as? Long)?.toInt() ?: -1
                                val poll = Poll(
                                    id = existing?.id ?: 0,
                                    question = data["question"] as? String ?: "",
                                    options = (data["options"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                                    createdByMemberId = createdByMemberId,
                                    createdAt = (data["createdAt"] as? Long) ?: 0L,
                                    isClosed = data["isClosed"] as? Boolean ?: false,
                                    mode = data["mode"] as? String ?: PollMode.VOTE,
                                    spinResultIndex = (data["spinResultIndex"] as? Long)?.toInt(),
                                    spinStartedAt = data["spinStartedAt"] as? Long,
                                    firestoreId = doc.id
                                )
                                val localId = pollRepository.insertPoll(poll)
                                val resolvedPollId = if (existing != null) existing.id else localId.toInt()
                                registerVotesListener(familyDoc, doc.id, resolvedPollId)
                                registerOptionsListener(familyDoc, doc.id, resolvedPollId)

                                if (change.type == DocumentChange.Type.ADDED && shouldNotify &&
                                    createdByMemberId != _activeMemberId.value
                                ) {
                                    val creatorName = data["createdByName"] as? String ?: "A family member"
                                    NotificationHelper.showNewPollNotification(getApplication(), creatorName, poll.question)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                pollRepository.getPollByFirestoreId(doc.id)?.let {
                                    pollRepository.deletePollById(it.id)
                                }
                                voteListeners.remove(doc.id)?.remove()
                                optionListeners.remove(doc.id)?.remove()
                            }
                        }
                    }
                }
            }
    }

    private fun registerOptionsListener(familyDoc: DocumentReference, pollFirestoreId: String, localPollId: Int) {
        if (optionListeners.containsKey(pollFirestoreId)) return

        optionListeners[pollFirestoreId] = familyDoc.collection("polls")
            .document(pollFirestoreId)
            .collection("options")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val existing = pollRepository.getPollOptionByFirestoreId(doc.id)
                                val option = PollOption(
                                    id = existing?.id ?: 0,
                                    pollId = localPollId,
                                    text = data["text"] as? String ?: "",
                                    createdByMemberId = (data["createdByMemberId"] as? Long)?.toInt() ?: -1,
                                    createdAt = (data["createdAt"] as? Long) ?: 0L,
                                    firestoreId = doc.id
                                )
                                pollRepository.insertOption(option)
                            }
                            DocumentChange.Type.REMOVED -> {
                                pollRepository.getPollOptionByFirestoreId(doc.id)?.let {
                                    pollRepository.deleteOptionById(it.id)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun registerVotesListener(familyDoc: DocumentReference, pollFirestoreId: String, localPollId: Int) {
        if (voteListeners.containsKey(pollFirestoreId)) return

        voteListeners[pollFirestoreId] = familyDoc.collection("polls")
            .document(pollFirestoreId)
            .collection("votes")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val memberId = change.document.id.toIntOrNull() ?: continue
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val data = change.document.data
                                val optionIndex = (data["optionIndex"] as? Long)?.toInt() ?: continue
                                val votedAt = (data["votedAt"] as? Long) ?: System.currentTimeMillis()
                                pollRepository.insertVote(
                                    PollVote(
                                        pollId = localPollId,
                                        memberId = memberId,
                                        optionIndex = optionIndex,
                                        votedAt = votedAt,
                                        firestoreId = "$pollFirestoreId:$memberId"
                                    )
                                )
                            }
                            DocumentChange.Type.REMOVED -> {
                                pollRepository.deleteVote(localPollId, memberId)
                            }
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        pollsListener?.remove()
        voteListeners.values.forEach { it.remove() }
        voteListeners.clear()
        optionListeners.values.forEach { it.remove() }
        optionListeners.clear()
    }
}

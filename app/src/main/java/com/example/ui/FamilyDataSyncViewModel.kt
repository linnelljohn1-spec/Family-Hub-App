package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalendarEvent
import com.example.data.CalendarRepository
import com.example.data.Contribution
import com.example.data.FamilyMember
import com.example.data.FamilyTask
import com.example.data.SavingsGoal
import com.example.data.SavingsRepository
import com.example.data.TaskRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

private const val NO_MEMBER = -1

class FamilyDataSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SavingsRepository(AppDatabase.getDatabase(application).savingsDao())
    private val calendarRepository = CalendarRepository(AppDatabase.getDatabase(application).calendarDao())
    private val taskRepository = TaskRepository(AppDatabase.getDatabase(application).taskDao())
    private val firestore = FirebaseFirestore.getInstance()

    private var syncGroupCode: String = ""

    private var membersListener: ListenerRegistration? = null
    private var goalsListener: ListenerRegistration? = null
    private var contributionsListener: ListenerRegistration? = null
    private var calendarEventsListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null

    // Docs whose parent (member/goal) hadn't synced locally yet when they first arrived.
    private val pendingGoalDocs = mutableListOf<DocumentSnapshot>()
    private val pendingContributionDocs = mutableListOf<DocumentSnapshot>()
    private val pendingCalendarEventDocs = mutableListOf<DocumentSnapshot>()
    private val pendingTaskDocs = mutableListOf<DocumentSnapshot>()

    fun setSyncGroupCode(code: String) {
        if (syncGroupCode == code) return
        syncGroupCode = code
        registerListeners(code)
    }

    private fun registerListeners(code: String) {
        membersListener?.remove()
        goalsListener?.remove()
        contributionsListener?.remove()
        calendarEventsListener?.remove()
        tasksListener?.remove()
        pendingGoalDocs.clear()
        pendingContributionDocs.clear()
        pendingCalendarEventDocs.clear()
        pendingTaskDocs.clear()

        if (code.isBlank()) return

        val familyDoc = firestore.collection("families").document(code)

        membersListener = familyDoc.collection("members").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            viewModelScope.launch {
                for (change in snapshot.documentChanges) {
                    handleMemberChange(change)
                }
                drainPendingGoals()
                drainPendingCalendarEvents()
                drainPendingTasks()
            }
        }

        goalsListener = familyDoc.collection("goals").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            viewModelScope.launch {
                for (change in snapshot.documentChanges) {
                    handleGoalChange(change)
                }
                drainPendingContributions()
            }
        }

        contributionsListener = familyDoc.collection("contributions").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            viewModelScope.launch {
                for (change in snapshot.documentChanges) {
                    handleContributionChange(change)
                }
            }
        }

        calendarEventsListener = familyDoc.collection("calendarEvents").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            viewModelScope.launch {
                for (change in snapshot.documentChanges) {
                    handleCalendarEventChange(change)
                }
            }
        }

        tasksListener = familyDoc.collection("tasks").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            viewModelScope.launch {
                for (change in snapshot.documentChanges) {
                    handleTaskChange(change)
                }
            }
        }
    }

    // ---- Members ----

    private suspend fun handleMemberChange(change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            repository.getMemberByFirestoreId(doc.id)?.let { repository.deleteMember(it) }
            return
        }
        val data = doc.data
        val existing = repository.getMemberByFirestoreId(doc.id)
        val member = FamilyMember(
            id = existing?.id ?: 0,
            name = data["name"] as? String ?: "",
            avatarColorHex = data["avatarColorHex"] as? String ?: "#6750A4",
            unallocatedBalance = data["unallocatedBalance"] as? Double ?: 0.0,
            isAdmin = data["isAdmin"] as? Boolean ?: false,
            password = data["password"] as? String ?: "1234",
            firestoreId = doc.id
        )
        if (existing != null) {
            // Use @Update, not REPLACE-insert: REPLACE would delete+reinsert the row,
            // cascading to delete this member's goals via the FK ON DELETE CASCADE.
            repository.updateMember(member)
        } else {
            repository.insertMember(member)
        }
    }

    // ---- Goals ----

    private suspend fun handleGoalChange(change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            repository.getGoalByFirestoreId(doc.id)?.let { repository.deleteGoal(it) }
            return
        }
        if (!tryUpsertGoal(doc)) {
            pendingGoalDocs.add(doc)
        }
    }

    private suspend fun tryUpsertGoal(doc: DocumentSnapshot): Boolean {
        val data = doc.data ?: return true
        val memberFirestoreId = data["memberFirestoreId"] as? String ?: return true
        val member = repository.getMemberByFirestoreId(memberFirestoreId) ?: return false
        val existing = repository.getGoalByFirestoreId(doc.id)
        val goal = SavingsGoal(
            id = existing?.id ?: 0,
            memberId = member.id,
            title = data["title"] as? String ?: "",
            targetAmount = data["targetAmount"] as? Double ?: 0.0,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            purchaseUrl = data["purchaseUrl"] as? String,
            firestoreId = doc.id
        )
        if (existing != null) {
            // @Update, not REPLACE-insert: REPLACE would cascade-delete this goal's contributions.
            repository.updateGoal(goal)
        } else {
            repository.insertGoal(goal)
        }
        return true
    }

    private suspend fun drainPendingGoals() {
        if (pendingGoalDocs.isEmpty()) return
        val docs = pendingGoalDocs.toList()
        pendingGoalDocs.clear()
        for (doc in docs) {
            if (!tryUpsertGoal(doc)) {
                pendingGoalDocs.add(doc)
            }
        }
    }

    // ---- Contributions ----

    private suspend fun handleContributionChange(change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            repository.getContributionByFirestoreId(doc.id)?.let { repository.deleteContribution(it) }
            return
        }
        if (!tryUpsertContribution(doc)) {
            pendingContributionDocs.add(doc)
        }
    }

    private suspend fun tryUpsertContribution(doc: DocumentSnapshot): Boolean {
        val data = doc.data ?: return true
        val goalFirestoreId = data["goalFirestoreId"] as? String ?: return true
        val goal = repository.getGoalByFirestoreId(goalFirestoreId) ?: return false
        val existing = repository.getContributionByFirestoreId(doc.id)
        val contribution = Contribution(
            id = existing?.id ?: 0,
            goalId = goal.id,
            amount = data["amount"] as? Double ?: 0.0,
            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
            note = data["note"] as? String,
            firestoreId = doc.id
        )
        // Contribution has no dependents, so REPLACE-insert is safe for both insert and update.
        repository.insertContribution(contribution)
        return true
    }

    private suspend fun drainPendingContributions() {
        if (pendingContributionDocs.isEmpty()) return
        val docs = pendingContributionDocs.toList()
        pendingContributionDocs.clear()
        for (doc in docs) {
            if (!tryUpsertContribution(doc)) {
                pendingContributionDocs.add(doc)
            }
        }
    }

    // ---- Calendar events ----

    private suspend fun handleCalendarEventChange(change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            calendarRepository.getEventByFirestoreId(doc.id)?.let { calendarRepository.deleteEvent(it) }
            return
        }
        if (!tryUpsertCalendarEvent(doc)) {
            pendingCalendarEventDocs.add(doc)
        }
    }

    private suspend fun tryUpsertCalendarEvent(doc: DocumentSnapshot): Boolean {
        val data = doc.data ?: return true
        val createdByFirestoreId = data["createdByMemberFirestoreId"] as? String
        val createdByMemberId = if (createdByFirestoreId == null) {
            NO_MEMBER
        } else {
            repository.getMemberByFirestoreId(createdByFirestoreId)?.id ?: return false
        }
        val event = CalendarEvent(
            id = calendarRepository.getEventByFirestoreId(doc.id)?.id ?: 0,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            date = data["date"] as? String ?: "",
            time = data["time"] as? String ?: "",
            category = data["category"] as? String ?: "Other",
            createdByMemberId = createdByMemberId,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            isAllDay = data["isAllDay"] as? Boolean ?: false,
            endTime = data["endTime"] as? String,
            firestoreId = doc.id
        )
        // No FK/CASCADE relationships declared on CalendarEvent, safe as REPLACE-insert.
        calendarRepository.insertEvent(event)
        return true
    }

    private suspend fun drainPendingCalendarEvents() {
        if (pendingCalendarEventDocs.isEmpty()) return
        val docs = pendingCalendarEventDocs.toList()
        pendingCalendarEventDocs.clear()
        for (doc in docs) {
            if (!tryUpsertCalendarEvent(doc)) {
                pendingCalendarEventDocs.add(doc)
            }
        }
    }

    // ---- Tasks ----

    private suspend fun handleTaskChange(change: DocumentChange) {
        val doc = change.document
        if (change.type == DocumentChange.Type.REMOVED) {
            taskRepository.getTaskByFirestoreId(doc.id)?.let { taskRepository.deleteTask(it) }
            return
        }
        if (!tryUpsertTask(doc)) {
            pendingTaskDocs.add(doc)
        }
    }

    private suspend fun tryUpsertTask(doc: DocumentSnapshot): Boolean {
        val data = doc.data ?: return true
        val assignedFirestoreId = data["assignedMemberFirestoreId"] as? String
        val assignedMemberId = if (assignedFirestoreId == null) {
            NO_MEMBER
        } else {
            repository.getMemberByFirestoreId(assignedFirestoreId)?.id ?: return false
        }
        val task = FamilyTask(
            id = taskRepository.getTaskByFirestoreId(doc.id)?.id ?: 0,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            assignedMemberId = assignedMemberId,
            dueDate = data["dueDate"] as? String ?: "",
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            firestoreId = doc.id
        )
        // No FK/CASCADE relationships declared on FamilyTask, safe as REPLACE-insert.
        taskRepository.insertTask(task)
        return true
    }

    private suspend fun drainPendingTasks() {
        if (pendingTaskDocs.isEmpty()) return
        val docs = pendingTaskDocs.toList()
        pendingTaskDocs.clear()
        for (doc in docs) {
            if (!tryUpsertTask(doc)) {
                pendingTaskDocs.add(doc)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        membersListener?.remove()
        goalsListener?.remove()
        contributionsListener?.remove()
        calendarEventsListener?.remove()
        tasksListener?.remove()
    }
}

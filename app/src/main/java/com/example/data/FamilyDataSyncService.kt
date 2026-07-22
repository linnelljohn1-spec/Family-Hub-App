package com.example.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FamilyDataSyncService {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private const val NO_MEMBER = -1

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    private fun familyDoc(syncGroupCode: String) =
        firestore.collection("families").document(syncGroupCode)

    // ---- Members ----

    fun upsertMember(syncGroupCode: String, member: FamilyMember) {
        val firestoreId = member.firestoreId ?: return
        val data = hashMapOf(
            "name" to member.name,
            "avatarColorHex" to member.avatarColorHex,
            "unallocatedBalance" to member.unallocatedBalance,
            "isAdmin" to member.isAdmin,
            "password" to member.password,
            "updatedAt" to System.currentTimeMillis()
        )
        familyDoc(syncGroupCode).collection("members").document(firestoreId)
            .set(data, SetOptions.merge())
    }

    suspend fun deleteMember(syncGroupCode: String, firestoreId: String) {
        // Mirrors Room's FamilyMember -> SavingsGoal CASCADE relationship.
        val dependentGoals = familyDoc(syncGroupCode).collection("goals")
            .whereEqualTo("memberFirestoreId", firestoreId)
            .get()
            .awaitResult()
        for (doc in dependentGoals.documents) {
            deleteGoal(syncGroupCode, doc.id)
        }
        familyDoc(syncGroupCode).collection("members").document(firestoreId).delete()
    }

    // ---- Goals ----

    fun upsertGoal(syncGroupCode: String, goal: SavingsGoal, memberFirestoreId: String) {
        val firestoreId = goal.firestoreId ?: return
        val data = hashMapOf(
            "memberFirestoreId" to memberFirestoreId,
            "title" to goal.title,
            "targetAmount" to goal.targetAmount,
            "createdAt" to goal.createdAt,
            "isCompleted" to goal.isCompleted,
            "purchaseUrl" to goal.purchaseUrl,
            "updatedAt" to System.currentTimeMillis()
        )
        familyDoc(syncGroupCode).collection("goals").document(firestoreId)
            .set(data, SetOptions.merge())
    }

    suspend fun deleteGoal(syncGroupCode: String, firestoreId: String) {
        // Firestore has no native cascade delete - remove dependent contributions first.
        val dependentContributions = familyDoc(syncGroupCode).collection("contributions")
            .whereEqualTo("goalFirestoreId", firestoreId)
            .get()
            .awaitResult()
        for (doc in dependentContributions.documents) {
            doc.reference.delete()
        }
        familyDoc(syncGroupCode).collection("goals").document(firestoreId).delete()
    }

    // ---- Contributions ----

    fun upsertContribution(syncGroupCode: String, contribution: Contribution, goalFirestoreId: String) {
        val firestoreId = contribution.firestoreId ?: return
        val data = hashMapOf(
            "goalFirestoreId" to goalFirestoreId,
            "amount" to contribution.amount,
            "timestamp" to contribution.timestamp,
            "note" to contribution.note,
            "updatedAt" to System.currentTimeMillis()
        )
        familyDoc(syncGroupCode).collection("contributions").document(firestoreId)
            .set(data, SetOptions.merge())
    }

    fun deleteContribution(syncGroupCode: String, firestoreId: String) {
        familyDoc(syncGroupCode).collection("contributions").document(firestoreId).delete()
    }

    // ---- Calendar events ----

    fun upsertCalendarEvent(syncGroupCode: String, event: CalendarEvent, createdByMemberFirestoreId: String?) {
        val firestoreId = event.firestoreId ?: return
        val data = hashMapOf(
            "title" to event.title,
            "description" to event.description,
            "date" to event.date,
            "time" to event.time,
            "category" to event.category,
            "createdByMemberFirestoreId" to createdByMemberFirestoreId,
            "createdAt" to event.createdAt,
            "isCompleted" to event.isCompleted,
            "isAllDay" to event.isAllDay,
            "endTime" to event.endTime,
            "repeatRule" to event.repeatRule,
            "seriesId" to event.seriesId,
            "updatedAt" to System.currentTimeMillis()
        )
        familyDoc(syncGroupCode).collection("calendarEvents").document(firestoreId)
            .set(data, SetOptions.merge())
    }

    fun deleteCalendarEvent(syncGroupCode: String, firestoreId: String) {
        familyDoc(syncGroupCode).collection("calendarEvents").document(firestoreId).delete()
    }

    // ---- Tasks ----

    fun upsertTask(syncGroupCode: String, task: FamilyTask, assignedMemberFirestoreId: String?) {
        val firestoreId = task.firestoreId ?: return
        val data = hashMapOf(
            "title" to task.title,
            "description" to task.description,
            "assignedMemberFirestoreId" to assignedMemberFirestoreId,
            "dueDate" to task.dueDate,
            "isCompleted" to task.isCompleted,
            "createdAt" to task.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )
        familyDoc(syncGroupCode).collection("tasks").document(firestoreId)
            .set(data, SetOptions.merge())
    }

    fun deleteTask(syncGroupCode: String, firestoreId: String) {
        familyDoc(syncGroupCode).collection("tasks").document(firestoreId).delete()
    }

    // ---- Backfill / migration safety ----

    suspend fun hasAnyRemoteData(syncGroupCode: String): Boolean {
        val collectionNames = listOf("members", "goals", "contributions", "calendarEvents", "tasks")
        for (name in collectionNames) {
            val snapshot = familyDoc(syncGroupCode).collection(name).limit(1).get().awaitResult()
            if (!snapshot.isEmpty) return true
        }
        return false
    }

    suspend fun backfill(
        syncGroupCode: String,
        members: List<FamilyMember>,
        goals: List<SavingsGoal>,
        contributions: List<Contribution>,
        events: List<CalendarEvent>,
        tasks: List<FamilyTask>
    ) {
        val memberIdToFirestoreId = members.associate { it.id to it.firestoreId }
        val goalIdToFirestoreId = goals.associate { it.id to it.firestoreId }

        for (member in members) {
            upsertMember(syncGroupCode, member)
        }
        for (goal in goals) {
            val memberFirestoreId = memberIdToFirestoreId[goal.memberId] ?: continue
            upsertGoal(syncGroupCode, goal, memberFirestoreId)
        }
        for (contribution in contributions) {
            val goalFirestoreId = goalIdToFirestoreId[contribution.goalId] ?: continue
            upsertContribution(syncGroupCode, contribution, goalFirestoreId)
        }
        for (event in events) {
            val createdByFirestoreId = if (event.createdByMemberId == NO_MEMBER) {
                null
            } else {
                memberIdToFirestoreId[event.createdByMemberId]
            }
            upsertCalendarEvent(syncGroupCode, event, createdByFirestoreId)
        }
        for (task in tasks) {
            val assignedFirestoreId = if (task.assignedMemberId == NO_MEMBER) {
                null
            } else {
                memberIdToFirestoreId[task.assignedMemberId]
            }
            upsertTask(syncGroupCode, task, assignedFirestoreId)
        }
    }
}

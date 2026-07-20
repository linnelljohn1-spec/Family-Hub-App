package com.example.data

import com.google.firebase.firestore.FirebaseFirestore

object GoalEventService {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun recordGoalReached(
        syncGroupCode: String,
        goalId: Int,
        memberId: Int,
        memberName: String,
        goalTitle: String
    ) {
        if (syncGroupCode.isBlank()) return
        val event = hashMapOf(
            "goalId" to goalId,
            "memberId" to memberId,
            "memberName" to memberName,
            "goalTitle" to goalTitle,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("families")
            .document(syncGroupCode)
            .collection("goalEvents")
            .add(event)
    }
}

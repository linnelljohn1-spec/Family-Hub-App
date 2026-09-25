package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat

object WalletEventService {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun recordDeposit(
        syncGroupCode: String,
        memberFirestoreId: String,
        memberName: String,
        amount: Double,
        byMemberFirestoreId: String?,
        byMemberName: String?
    ) {
        if (syncGroupCode.isBlank()) return
        val event = hashMapOf(
            "memberFirestoreId" to memberFirestoreId,
            "memberName" to memberName,
            "amount" to amount,
            // Pre-formatted in this device's currency so the push text matches the app.
            "amountLabel" to NumberFormat.getCurrencyInstance().format(amount),
            "byMemberFirestoreId" to byMemberFirestoreId,
            "byMemberName" to byMemberName,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("families")
            .document(syncGroupCode)
            .collection("walletEvents")
            .add(event)
    }
}

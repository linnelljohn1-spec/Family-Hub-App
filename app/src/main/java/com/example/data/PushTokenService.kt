package com.example.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Keeps families/{code}/devices/{fcmToken} in sync with which member is using this device,
 * so the Cloud Functions know which tokens to push to.
 */
object PushTokenService {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private const val PREFS_NAME = "push_prefs"
    private const val KEY_CODE = "registered_sync_group_code"
    private const val KEY_MEMBER = "registered_member_firestore_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun devicesCollection(syncGroupCode: String) =
        firestore.collection("families").document(syncGroupCode).collection("devices")

    /** Registers this device for [memberFirestoreId], or unregisters it when [syncGroupCode] is blank. */
    fun registerDevice(context: Context, syncGroupCode: String, memberFirestoreId: String?) {
        // Members may still be loading - wait for a real member rather than unregistering.
        if (syncGroupCode.isNotBlank() && memberFirestoreId.isNullOrBlank()) return

        val appContext = context.applicationContext
        val previousCode = prefs(appContext).getString(KEY_CODE, "") ?: ""

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (previousCode.isNotBlank() && previousCode != syncGroupCode) {
                devicesCollection(previousCode).document(token).delete()
            }
            if (syncGroupCode.isBlank() || memberFirestoreId.isNullOrBlank()) {
                prefs(appContext).edit().remove(KEY_CODE).remove(KEY_MEMBER).apply()
                return@addOnSuccessListener
            }
            writeDevice(syncGroupCode, memberFirestoreId, token)
            prefs(appContext).edit()
                .putString(KEY_CODE, syncGroupCode)
                .putString(KEY_MEMBER, memberFirestoreId)
                .apply()
        }
    }

    /** Called when FCM rotates the token - re-registers with the last known family/member. */
    fun onTokenRefreshed(context: Context, token: String) {
        val prefs = prefs(context.applicationContext)
        val code = prefs.getString(KEY_CODE, "") ?: ""
        val memberFirestoreId = prefs.getString(KEY_MEMBER, "") ?: ""
        if (code.isBlank() || memberFirestoreId.isBlank()) return
        writeDevice(code, memberFirestoreId, token)
    }

    private fun writeDevice(syncGroupCode: String, memberFirestoreId: String, token: String) {
        val data = hashMapOf(
            "memberFirestoreId" to memberFirestoreId,
            "platform" to "android",
            "updatedAt" to System.currentTimeMillis()
        )
        devicesCollection(syncGroupCode).document(token).set(data)
    }
}

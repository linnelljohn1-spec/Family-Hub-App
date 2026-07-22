package com.example.update

import android.content.Context

/** Remembers which update version the user dismissed, so the banner doesn't reappear for it. */
object UpdatePreferences {
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"

    fun getDismissedVersionCode(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DISMISSED_VERSION_CODE, -1)

    fun setDismissedVersionCode(context: Context, versionCode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED_VERSION_CODE, versionCode)
            .apply()
    }
}

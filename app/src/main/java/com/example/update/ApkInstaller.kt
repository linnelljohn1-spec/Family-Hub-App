package com.example.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File

/** Handles the "install unknown apps" permission check and driving the install itself. */
object ApkInstaller {
    // Session API instead of ACTION_VIEW: some OEM ROMs (seen on Samsung) can show a
    // completed install UI via ACTION_VIEW without the update actually applying, with no
    // way to detect it. Session commits report a real, verifiable status via broadcast.
    const val ACTION_INSTALL_RESULT = "com.example.update.ACTION_INSTALL_RESULT"

    fun canRequestInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /** Intent to the system settings screen where the user grants "install unknown apps" for this app. */
    fun installPermissionSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    /** Result arrives asynchronously via a broadcast with action [ACTION_INSTALL_RESULT]. */
    fun installApk(context: Context, apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        session.use {
            apkFile.inputStream().use { input ->
                it.openWrite("update", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    it.fsync(output)
                }
            }

            val resultIntent = Intent(ACTION_INSTALL_RESULT).setPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, resultIntent, flags)
            it.commit(pendingIntent.intentSender)
        }
    }
}

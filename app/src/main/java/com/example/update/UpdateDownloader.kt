package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import java.io.File

/** Downloads the update APK via the system DownloadManager into app-private external storage. */
object UpdateDownloader {
    private const val APK_FILE_NAME = "update.apk"
    private const val POLL_INTERVAL_MS = 500L

    fun downloadedApkFile(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)

    fun enqueueDownload(context: Context, url: String): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Family Hub update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
        return downloadManager.enqueue(request)
    }

    /** Polls DownloadManager until the given download finishes; returns true on success. */
    suspend fun awaitCompletion(context: Context, downloadId: Long): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> return true
                        DownloadManager.STATUS_FAILED -> return false
                    }
                } else {
                    return false
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}

package com.vinithreddybanda.whatsapstatus.data

import android.os.Environment
import com.vinithreddybanda.whatsapstatus.model.Status
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class StatusRepository {

    // Check multiple potential paths for WhatsApp Statuses
    private val targetPaths: List<File>
        get() {
            val externalDir = Environment.getExternalStorageDirectory()
            return listOf(
                // Android 11+ Standard Path
                File(externalDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                // Legacy Path (Android 10 and below)
                File(externalDir, "WhatsApp/Media/.Statuses"),
                // WhatsApp Business Android 11+
                File(externalDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                // WhatsApp Business Legacy
                File(externalDir, "WhatsApp Business/Media/.Statuses"),
                // Additional potential paths (Explicit paths as fallback)
                File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File("/storage/emulated/0/WhatsApp/Media/.Statuses")
            )
        }

    // Path where WE save the statuses
    private val savedPath = File(
        Environment.getExternalStorageDirectory(), "Documents/StatusSaver"
    )

    suspend fun getStatuses(): List<Status> = withContext(Dispatchers.IO) {
        val statusList = ArrayList<Status>()
        val uniquePaths = HashSet<String>() // To avoid duplicates if paths overlap or symlink

        for (path in targetPaths) {
            // Log.d("StatusRepo", "Checking path: ${path.absolutePath}, exists: ${path.exists()}, isDir: ${path.isDirectory}")
            if (path.exists() && path.isDirectory) {
                val files = path.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.exists() && !uniquePaths.contains(file.absolutePath) &&
                            (file.name.endsWith(".jpg") || file.name.endsWith(".gif") || file.name.endsWith(".mp4"))) {

                            statusList.add(
                                Status(
                                    file = file,
                                    title = file.name,
                                    path = file.absolutePath,
                                    isVideo = file.name.endsWith(".mp4")
                                )
                            )
                            uniquePaths.add(file.absolutePath)
                        }
                    }
                }
            }
        }
        // Return sorted by newest first
        statusList.sortedByDescending { it.file.lastModified() }
    }

    suspend fun getSavedStatuses(): List<Status> = withContext(Dispatchers.IO) {
        val statusList = ArrayList<Status>()
        if (savedPath.exists() && savedPath.isDirectory) {
             val files = savedPath.listFiles()
             if (files != null) {
                 for (file in files) {
                      if (file.name.endsWith(".jpg") || file.name.endsWith(".gif") || file.name.endsWith(".mp4")) {
                        statusList.add(
                            Status(
                                file = file,
                                title = file.name,
                                path = file.absolutePath,
                                isVideo = file.name.endsWith(".mp4")
                            )
                        )
                    }
                 }
             }
        }
        statusList.sortedByDescending { it.file.lastModified() }
    }

    suspend fun saveStatus(status: Status): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!savedPath.exists()) {
                savedPath.mkdirs()
            }
            val destFile = File(savedPath, status.title)
            status.file.copyTo(destFile, overwrite = true)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteStatus(status: Status): Boolean = withContext(Dispatchers.IO) {
        try {
            if (status.file.exists()) {
                status.file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
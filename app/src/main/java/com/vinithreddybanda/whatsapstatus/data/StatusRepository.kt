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

    // Supported extensions for filtering
    private val supportedExtensions = setOf("jpg", "gif", "mp4")

    // Extension property for file type checking
    private val File.isSupportedMedia: Boolean
        get() = extension.lowercase() in supportedExtensions

    private val File.isVideoFile: Boolean
        get() = extension.equals("mp4", ignoreCase = true)

    suspend fun getStatuses(): List<Status> = withContext(Dispatchers.IO) {
        val uniquePaths = HashSet<String>() // To avoid duplicates if paths overlap or symlink
        val statusList = mutableListOf<Status>()

        for (path in targetPaths) {
            // Log.d("StatusRepo", "Checking path: ${path.absolutePath}, exists: ${path.exists()}, isDir: ${path.isDirectory}")
            if (path.exists() && path.isDirectory) {
                path.listFiles()?.forEach { file ->
                    // Use HashSet.add() return value - returns true if element was added (wasn't present before)
                    if (file.isSupportedMedia && uniquePaths.add(file.absolutePath)) {
                        statusList.add(
                            Status(
                                file = file,
                                title = file.name,
                                path = file.absolutePath,
                                isVideo = file.isVideoFile
                            )
                        )
                    }
                }
            }
        }
        // Return sorted by newest first
        statusList.sortedByDescending { it.file.lastModified() }
    }

    suspend fun getSavedStatuses(): List<Status> = withContext(Dispatchers.IO) {
        if (!savedPath.exists() || !savedPath.isDirectory) {
            return@withContext emptyList()
        }

        savedPath.listFiles()
            ?.filter { it.isSupportedMedia }
            ?.map { file ->
                Status(
                    file = file,
                    title = file.name,
                    path = file.absolutePath,
                    isVideo = file.isVideoFile
                )
            }
            ?.sortedByDescending { it.file.lastModified() }
            ?: emptyList()
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
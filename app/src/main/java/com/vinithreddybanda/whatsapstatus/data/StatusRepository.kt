package com.vinithreddybanda.whatsapstatus.data

import android.os.Environment
import com.vinithreddybanda.whatsapstatus.model.Status
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatusRepository {

    private val targetPaths: List<File>
        get() {
            val root = Environment.getExternalStorageDirectory()
            return listOf(
                File(root, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(root, "WhatsApp/Media/.Statuses"),
                File(root, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                File(root, "WhatsApp Business/Media/.Statuses")
            )
        }

    private val savedPath = File(
        Environment.getExternalStorageDirectory(),
        "Documents/StatusSaver"
    )

    private val supportedExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "mp4"
    )

    private val File.isSupportedMedia: Boolean
        get() = extension.lowercase() in supportedExtensions

    private val File.isVideoFile: Boolean
        get() = extension.equals("mp4", ignoreCase = true)

    suspend fun getStatuses(): List<Status> = withContext(Dispatchers.IO) {
        val seen = HashSet<String>()
        val statuses = ArrayList<Status>()

        targetPaths.forEach { directory ->
            if (!directory.isDirectory || !directory.canRead()) return@forEach

            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.isSupportedMedia && seen.add(file.absolutePath)) {
                    statuses += Status(
                        file = file,
                        title = file.name,
                        path = file.absolutePath,
                        isVideo = file.isVideoFile,
                        timestamp = file.lastModified()
                    )
                }
            }
        }

        statuses.sortByDescending { it.timestamp }
        statuses
    }

    suspend fun getSavedStatuses(): List<Status> = withContext(Dispatchers.IO) {
        if (!savedPath.isDirectory || !savedPath.canRead()) return@withContext emptyList()

        savedPath.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.isSupportedMedia }
            ?.map { file ->
                Status(
                    file = file,
                    title = file.name,
                    path = file.absolutePath,
                    isVideo = file.isVideoFile,
                    timestamp = file.lastModified()
                )
            }
            ?.sortedByDescending { it.timestamp }
            ?.toList()
            ?: emptyList()
    }

    suspend fun saveStatus(status: Status): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!savedPath.exists() && !savedPath.mkdirs()) return@withContext false

            val original = File(savedPath, status.title)
            val destination = if (original.exists()) {
                val extension = original.extension
                val baseName = original.nameWithoutExtension
                File(savedPath, "$baseName-${status.timestamp}.$extension")
            } else {
                original
            }

            status.file.copyTo(destination, overwrite = true)
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun deleteStatus(status: Status): Boolean = withContext(Dispatchers.IO) {
        try {
            status.file.exists() && status.file.delete()
        } catch (_: SecurityException) {
            false
        }
    }
}

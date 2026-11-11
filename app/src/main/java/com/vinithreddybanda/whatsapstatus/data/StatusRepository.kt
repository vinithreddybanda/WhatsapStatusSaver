package com.vinithreddybanda.whatsapstatus.data

import android.graphics.BitmapFactory
import android.os.Environment
import com.vinithreddybanda.whatsapstatus.model.Status
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class StatusRepository {

    private val targetPaths: List<File>
        get() {
            val externalDir = Environment.getExternalStorageDirectory()
            return listOf(
                File(externalDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(externalDir, "WhatsApp/Media/.Statuses"),
                File(externalDir, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                File(externalDir, "WhatsApp Business/Media/.Statuses"),
                File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File("/storage/emulated/0/WhatsApp/Media/.Statuses")
            )
        }

    private val savedPath = File(
        Environment.getExternalStorageDirectory(),
        "Documents/StatusSaver"
    )

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val videoExtensions = setOf("mp4", "m4v", "3gp", "webm")
    private val supportedExtensions = imageExtensions + videoExtensions

    private val File.isSupportedMedia: Boolean
        get() = extension.lowercase(Locale.ROOT) in supportedExtensions

    private val File.isVideoFile: Boolean
        get() = extension.lowercase(Locale.ROOT) in videoExtensions

    /**
     * Emits sorted media in small batches. The UI can populate progressively instead
     * of waiting for one large scan result before showing the gallery.
     */
    fun streamStatuses(): Flow<List<Status>> = flow {
        val uniqueFiles = LinkedHashMap<String, File>()

        targetPaths.forEach { directory ->
            if (directory.isDirectory) {
                directory.listFiles()
                    ?.asSequence()
                    ?.filter { it.isFile && it.isSupportedMedia }
                    ?.forEach { file -> uniqueFiles.putIfAbsent(file.absolutePath, file) }
            }
        }

        val sortedFiles = uniqueFiles.values.sortedByDescending { it.lastModified() }
        sortedFiles.chunked(24).forEach { batch ->
            emit(batch.map(::toStatus))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSavedStatuses(): List<Status> = withContext(Dispatchers.IO) {
        if (!savedPath.isDirectory) return@withContext emptyList()

        savedPath.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.isSupportedMedia }
            ?.sortedByDescending { it.lastModified() }
            ?.map(::toStatus)
            ?.toList()
            ?: emptyList()
    }

    suspend fun saveStatus(status: Status): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!savedPath.exists() && !savedPath.mkdirs()) return@withContext false
            val destination = File(savedPath, status.title)
            status.file.copyTo(destination, overwrite = true)
            true
        } catch (_: Exception) {
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

    private fun toStatus(file: File): Status {
        val isVideo = file.isVideoFile
        return Status(
            file = file,
            title = file.name,
            path = file.absolutePath,
            isVideo = isVideo,
            aspectRatio = if (isVideo) null else readImageAspectRatio(file)
        )
    }

    /**
     * Reads only image bounds. It does not decode the full bitmap, so cards can
     * reserve their real, variable height before Coil loads the thumbnail.
     */
    private fun readImageAspectRatio(file: File): Float? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth.toFloat() / options.outHeight.toFloat()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

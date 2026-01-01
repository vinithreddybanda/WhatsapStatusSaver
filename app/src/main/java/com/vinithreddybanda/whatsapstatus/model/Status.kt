package com.vinithreddybanda.whatsapstatus.model

import java.io.File

data class Status(
    val file: File,
    val title: String,
    val path: String,
    val isVideo: Boolean,
    val timestamp: Long = file.lastModified() // Added timestamp field
)
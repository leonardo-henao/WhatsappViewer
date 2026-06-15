package com.wappscorp.wpvw.data.model

import java.io.File

enum class MediaType {
    IMAGE,
    AUDIO,
    VIDEO
}

data class WhatsAppMedia(
    val id: String,
    val name: String,
    val file: File,
    val type: MediaType,
    val size: Long,
    val lastModified: Long,
    var isSelected: Boolean = false
) {
    val sizeFormatted: String
        get() {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0))
                else -> String.format(java.util.Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            }
        }
}

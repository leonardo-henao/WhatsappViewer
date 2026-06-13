package com.wappscorp.wpvw.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wappscorp.wpvw.data.model.MediaType
import com.wappscorp.wpvw.data.model.WhatsAppMedia
import java.io.File
import java.util.UUID

class MediaRepository(private val context: Context) {

    private val basePath: String
        get() = "${Environment.getExternalStorageDirectory().path}/Android/media/com.whatsapp/WhatsApp/Media"

    fun loadImages(): List<WhatsAppMedia> {
        val result = loadFromDirectFile("WhatsApp Images", MediaType.IMAGE)
        if (result.isNotEmpty()) return result
        return loadFromMediaStore(MediaType.IMAGE)
    }

    fun loadVideos(): List<WhatsAppMedia> {
        val result = loadFromDirectFile("WhatsApp Video", MediaType.VIDEO)
        if (result.isNotEmpty()) return result
        return loadFromMediaStore(MediaType.VIDEO)
    }

    fun loadAudios(): List<WhatsAppMedia> {
        val result = loadFromDirectFile("WhatsApp Audio", MediaType.AUDIO)
        if (result.isNotEmpty()) return result
        return loadFromMediaStore(MediaType.AUDIO)
    }

    fun loadMedia(): List<WhatsAppMedia> {
        val all = mutableListOf<WhatsAppMedia>()
        all.addAll(loadImages())
        all.addAll(loadVideos())
        all.addAll(loadAudios())
        return all.sortedByDescending { it.lastModified }
    }

    private fun loadFromDirectFile(folder: String, type: MediaType): List<WhatsAppMedia> {
        val result = mutableListOf<WhatsAppMedia>()
        for (sub in listOf("Sent", "Private")) {
            val dir = File("$basePath/$folder/$sub")
            if (dir.isDirectory) {
                val files = dir.listFiles()
                if (files != null) {
                    for (f in files) {
                        if (f.isFile && hasValidExtension(f.name, type)) {
                            result.add(WhatsAppMedia(UUID.randomUUID().toString(), f.name, f, type, f.length(), f.lastModified()))
                        }
                    }
                }
            }
        }
        return result
    }

    private fun loadFromMediaStore(type: MediaType): List<WhatsAppMedia> {
        val uri = when (type) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val result = mutableListOf<WhatsAppMedia>()

        try {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            val cursor = context.contentResolver.query(uri, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataIdx = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                if (nameIdx < 0) return@use

                while (c.moveToNext()) {
                    val name = c.getString(nameIdx) ?: continue
                    if (!hasValidExtension(name, type)) continue

                    var file: File? = null
                    if (dataIdx >= 0) {
                        val path = c.getString(dataIdx)
                        if (path != null) file = File(path)
                    }
                    val f = file ?: continue
                    val path = f.absolutePath

                    val folder = mediaFolder(type)
                    if (!path.contains("/WhatsApp/Media/$folder/Sent/") && !path.contains("/WhatsApp/Media/$folder/Private/")) continue
                    if (!f.exists()) continue

                    result.add(
                        WhatsAppMedia(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            file = f,
                            type = type,
                            size = if (sizeIdx >= 0) c.getLong(sizeIdx) else f.length(),
                            lastModified = if (dateIdx >= 0) c.getLong(dateIdx) * 1000 else f.lastModified()
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return result
    }

    private fun mediaFolder(type: MediaType): String = when (type) {
        MediaType.IMAGE -> "WhatsApp Images"
        MediaType.VIDEO -> "WhatsApp Video"
        MediaType.AUDIO -> "WhatsApp Audio"
    }

    private fun hasValidExtension(name: String, type: MediaType): Boolean {
        val ext = name.substringAfterLast('.').lowercase()
        return when (type) {
            MediaType.IMAGE -> ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
            MediaType.VIDEO -> ext in listOf("mp4", "3gp", "avi", "mkv", "mov", "webm")
            MediaType.AUDIO -> ext in listOf("mp3", "wav", "aac", "ogg", "m4a", "opus", "amr")
        }
    }

    fun deleteMedia(media: WhatsAppMedia): Boolean = media.file.delete()
    fun deleteMultipleMedia(mediaList: List<WhatsAppMedia>): Int {
        var d = 0; for (m in mediaList) { if (m.file.delete()) d++ }; return d
    }
    fun getTotalSize(mediaList: List<WhatsAppMedia>): Long = mediaList.sumOf { it.size }
}

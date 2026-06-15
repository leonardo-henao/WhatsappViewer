package com.wappscorp.wpvw.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wappscorp.wpvw.data.model.MediaType
import com.wappscorp.wpvw.data.model.WhatsAppMedia
import com.wappscorp.wpvw.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DeleteAction {
    data object Selected : DeleteAction()
    data class ByType(val type: MediaType) : DeleteAction()
    data object All : DeleteAction()
}

data class MediaUiState(
    val images: List<WhatsAppMedia> = emptyList(),
    val audios: List<WhatsAppMedia> = emptyList(),
    val videos: List<WhatsAppMedia> = emptyList(),
    val totalSize: Long = 0,
    val imagesSize: Long = 0,
    val audiosSize: Long = 0,
    val videosSize: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectionMode: Boolean = false,
    val selectedCount: Int = 0,
    val selectedMedia: WhatsAppMedia? = null
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _pendingDeleteAction = MutableStateFlow<DeleteAction?>(null)
    val pendingDeleteAction: StateFlow<DeleteAction?> = _pendingDeleteAction.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val images = repository.loadImages()
                    val audios = repository.loadAudios()
                    val videos = repository.loadVideos()
                    Triple(images, audios, videos)
                }
                _uiState.update {
                    it.copy(
                        images = result.first,
                        audios = result.second,
                        videos = result.third,
                        imagesSize = result.first.sumOf { m -> m.size },
                        audiosSize = result.second.sumOf { m -> m.size },
                        videosSize = result.third.sumOf { m -> m.size },
                        totalSize = result.first.sumOf { m -> m.size } +
                                result.second.sumOf { m -> m.size } +
                                result.third.sumOf { m -> m.size },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun requestDeleteSelected() {
        _pendingDeleteAction.value = DeleteAction.Selected
    }

    fun requestDeleteAllByType(type: MediaType) {
        _pendingDeleteAction.value = DeleteAction.ByType(type)
    }

    fun requestDeleteAll() {
        _pendingDeleteAction.value = DeleteAction.All
    }

    fun executePendingDelete() {
        val action = _pendingDeleteAction.value ?: return
        _pendingDeleteAction.value = null
        viewModelScope.launch {
            when (action) {
                is DeleteAction.Selected -> {
                    val selectedItems = getSelectedMedia()
                    withContext(Dispatchers.IO) { repository.deleteMultipleMedia(selectedItems) }
                }
                is DeleteAction.ByType -> {
                    val items = when (action.type) {
                        MediaType.IMAGE -> _uiState.value.images
                        MediaType.AUDIO -> _uiState.value.audios
                        MediaType.VIDEO -> _uiState.value.videos
                    }
                    withContext(Dispatchers.IO) { repository.deleteMultipleMedia(items) }
                }
                is DeleteAction.All -> {
                    val allMedia = _uiState.value.images + _uiState.value.audios + _uiState.value.videos
                    withContext(Dispatchers.IO) { repository.deleteMultipleMedia(allMedia) }
                }
            }
            clearSelection()
            loadMedia()
        }
    }

    fun deleteMedia(media: WhatsAppMedia) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteMedia(media) }
            clearSelectedMedia()
            loadMedia()
        }
    }

    fun toggleSelection(media: WhatsAppMedia) {
        _uiState.update { state ->
            val updatedList = when (media.type) {
                MediaType.IMAGE -> state.images.map {
                    if (it.id == media.id) it.copy(isSelected = !it.isSelected) else it
                }
                MediaType.AUDIO -> state.audios.map {
                    if (it.id == media.id) it.copy(isSelected = !it.isSelected) else it
                }
                MediaType.VIDEO -> state.videos.map {
                    if (it.id == media.id) it.copy(isSelected = !it.isSelected) else it
                }
            }
            val count = countSelected(
                if (media.type == MediaType.IMAGE) updatedList else state.images,
                if (media.type == MediaType.AUDIO) updatedList else state.audios,
                if (media.type == MediaType.VIDEO) updatedList else state.videos
            )
            when (media.type) {
                MediaType.IMAGE -> state.copy(images = updatedList, selectionMode = count > 0, selectedCount = count)
                MediaType.AUDIO -> state.copy(audios = updatedList, selectionMode = count > 0, selectedCount = count)
                MediaType.VIDEO -> state.copy(videos = updatedList, selectionMode = count > 0, selectedCount = count)
            }
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allImages = state.images.map { it.copy(isSelected = true) }
            val allAudios = state.audios.map { it.copy(isSelected = true) }
            val allVideos = state.videos.map { it.copy(isSelected = true) }
            val total = allImages.size + allAudios.size + allVideos.size
            state.copy(
                images = allImages,
                audios = allAudios,
                videos = allVideos,
                selectionMode = true,
                selectedCount = total
            )
        }
    }

    fun deselectAll() {
        _uiState.update { state ->
            state.copy(
                images = state.images.map { it.copy(isSelected = false) },
                audios = state.audios.map { it.copy(isSelected = false) },
                videos = state.videos.map { it.copy(isSelected = false) },
                selectionMode = false,
                selectedCount = 0
            )
        }
    }

    fun clearSelection() {
        deselectAll()
    }

    fun selectMediaForViewing(media: WhatsAppMedia) {
        _uiState.update { it.copy(selectedMedia = media) }
    }

    fun clearSelectedMedia() {
        _uiState.update { it.copy(selectedMedia = null) }
    }

    fun getSelectedMedia(): List<WhatsAppMedia> {
        val state = _uiState.value
        return state.images.filter { it.isSelected } +
                state.audios.filter { it.isSelected } +
                state.videos.filter { it.isSelected }
    }

    private fun countSelected(images: List<WhatsAppMedia>, audios: List<WhatsAppMedia>, videos: List<WhatsAppMedia>): Int {
        return images.count { it.isSelected } + audios.count { it.isSelected } + videos.count { it.isSelected }
    }
}

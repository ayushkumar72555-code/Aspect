package com.ayush.aspect.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.aspect.core.data.MediaItem
import com.ayush.aspect.core.data.MediaStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineUiState(
    val isLoading: Boolean = true,
    val items: List<MediaItem> = emptyList(),
    val error: String? = null,
    val selectedIds: Set<Long> = emptySet()
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val selectedCount: Int get() = selectedIds.size
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: MediaStoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.getMedia() }
                .onSuccess { media -> _uiState.value = TimelineUiState(isLoading = false, items = media) }
                .onFailure { throwable ->
                    _uiState.value = TimelineUiState(
                        isLoading = false,
                        error = throwable.message ?: "Unable to read your media."
                    )
                }
        }
    }

    fun toggleSelection(itemId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedIds = _uiState.value.selectedIds.toMutableSet().apply {
                if (!add(itemId)) remove(itemId)
            }
        )
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            selectedIds = _uiState.value.items.mapTo(mutableSetOf()) { it.id }
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun selectedItems(): List<MediaItem> =
        _uiState.value.items.filter { it.id in _uiState.value.selectedIds }

    fun favoriteSelected(favorite: Boolean) {
        val selected = selectedItems()
        if (selected.isEmpty()) return
        viewModelScope.launch { repository.setFavorite(selected, favorite) }
    }
}

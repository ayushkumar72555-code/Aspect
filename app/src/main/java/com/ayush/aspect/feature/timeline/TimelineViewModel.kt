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
    val error: String? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: MediaStoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.getMedia() }
                .onSuccess { media ->
                    _uiState.value = TimelineUiState(isLoading = false, items = media)
                }
                .onFailure { throwable ->
                    _uiState.value = TimelineUiState(
                        isLoading = false,
                        error = throwable.message ?: "Unable to read your media."
                    )
                }
        }
    }
}

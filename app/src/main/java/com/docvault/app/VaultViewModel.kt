package com.docvault.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docvault.app.data.SavedItem
import com.docvault.app.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application)

    private val pendingShareUris = MutableStateFlow<List<Uri>>(emptyList())

    val items = repository.items.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbar.asStateFlow()

    fun queueShareFromIntent(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingShareUris.value = uris
    }

    fun tryFlushPendingShares(lockEnabled: Boolean, unlocked: Boolean) {
        if (lockEnabled && !unlocked) return
        val uris = pendingShareUris.value
        if (uris.isEmpty()) return
        pendingShareUris.value = emptyList()
        importUris(uris)
    }

    fun import(uri: Uri?) {
        if (uri == null) {
            _snackbar.value = "Nothing selected"
            return
        }
        importUris(listOf(uri))
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _snackbar.value = "Nothing selected"
            return
        }
        viewModelScope.launch {
            var saved = 0
            var dup = 0
            var failed = 0
            for (uri in uris) {
                val result = repository.importFromUri(uri)
                when {
                    result.isSuccess -> saved++
                    result.exceptionOrNull()?.message == VaultRepository.DUPLICATE_FILE_MESSAGE -> dup++
                    else -> failed++
                }
            }
            _snackbar.value = when {
                uris.size == 1 && saved == 1 -> "Saved to vault"
                uris.size == 1 && dup == 1 -> "This file is already in your vault"
                uris.size == 1 && failed == 1 ->
                    "Could not import file"
                else -> buildString {
                    append("Done: $saved saved")
                    if (dup > 0) append(", $dup duplicate(s) skipped")
                    if (failed > 0) append(", $failed failed")
                }
            }
        }
    }

    fun dismissSnackbar() {
        _snackbar.value = null
    }

    fun delete(item: SavedItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun resolveFile(item: SavedItem) = repository.fileFor(item)
    fun resolveThumb(item: SavedItem) = repository.thumbFile(item)
}

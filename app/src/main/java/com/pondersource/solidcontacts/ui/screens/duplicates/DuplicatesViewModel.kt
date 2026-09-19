package com.pondersource.solidcontacts.ui.screens.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.repository.ContactsRepository
import com.pondersource.solidcontacts.domain.model.DuplicateCluster
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val loading: Boolean = true,
    val clusters: List<DuplicateCluster> = emptyList(),
    val dismissed: Set<String> = emptySet(),
    val merging: String? = null,
    val message: String? = null,
) {
    val visible: List<DuplicateCluster> get() = clusters.filterNot { it.signature in dismissed }
}

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicatesUiState())
    val state: StateFlow<DuplicatesUiState> = _state.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val clusters = contactsRepository.findDuplicates()
            _state.update { it.copy(loading = false, clusters = clusters) }
        }
    }

    /** Folds a cluster into its first member, which the finder ranks as the fullest record. */
    fun merge(cluster: DuplicateCluster, survivorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(merging = cluster.signature) }
            val losers = cluster.members.map { it.id }.filterNot { it == survivorId }
            contactsRepository.mergeContacts(survivorId, losers)
            _state.update {
                it.copy(
                    merging = null,
                    dismissed = it.dismissed + cluster.signature,
                    message = "Merged into one contact",
                )
            }
            scan()
        }
    }

    /** Keeps the two records apart. They are not the same person after all. */
    fun keepSeparate(cluster: DuplicateCluster) {
        _state.update { it.copy(dismissed = it.dismissed + cluster.signature) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}

package com.example.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val vault: VaultRepository, private val auth: AuthRepository) : ViewModel() {
    val state = combine(vault.contents, vault.ready, auth.currentUser, vault.loadError) { contents, ready, user, error ->
        MainUiState(contents, ready, user, error)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())
    private val channel = Channel<MainUiEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    fun onEvent(event: MainUiEvent) {
        when(event) {
            MainUiEvent.Foreground -> vault.requestSync()
            MainUiEvent.SignOut -> auth.signOut()
            is MainUiEvent.DeleteRecord -> viewModelScope.launch {
                try { vault.delete(event.id) }
                catch(e: CancellationException) { throw e }
                catch(e: Exception) { channel.send(MainUiEffect.Message("Không thể xóa. Hãy thử lại.")) }
            }
        }
    }
}

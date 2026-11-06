package com.example.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(available = auth.isAvailable.value && auth.webClientId.isNotBlank(), clientId = auth.webClientId))
    val state = _state.asStateFlow()
    init { viewModelScope.launch { auth.isAvailable.collect { available -> _state.update { it.copy(available = available && auth.webClientId.isNotBlank()) } } } }
    fun onEvent(event: LoginUiEvent) {
        when(event) {
            LoginUiEvent.Start -> _state.update { it.copy(busy = true, error = null) }
            LoginUiEvent.Cancel -> _state.update { it.copy(busy = false) }
            is LoginUiEvent.Failed -> _state.update { it.copy(busy = false, error = if (event.noAccount) "Thêm tài khoản Google trên thiết bị rồi thử lại." else "Đăng nhập chưa hoàn tất. Hãy thử lại.") }
            is LoginUiEvent.TokenReceived -> viewModelScope.launch {
                try { auth.authenticate(event.token) }
                catch(e: CancellationException) { throw e }
                catch(e: Exception) { _state.update { it.copy(error = "Không thể đăng nhập. Kiểm tra kết nối rồi thử lại.") } }
                finally { _state.update { it.copy(busy = false) } }
            }
        }
    }
}

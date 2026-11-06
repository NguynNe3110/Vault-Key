package com.example.domain.repository

import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserSession?>
    val isAvailable: StateFlow<Boolean>
    val webClientId: String
    suspend fun authenticate(token: String)
    fun signOut()
}

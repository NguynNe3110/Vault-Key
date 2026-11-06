package com.example.data.repository

import android.content.Context
import com.example.data.remote.datasource.FirebaseDataSource
import com.example.domain.repository.AuthRepository

class AuthRepositoryImpl(context: Context, private val source: FirebaseDataSource) : AuthRepository {
    override val currentUser = source.currentUser
    override val isAvailable = source.isFirebaseConfigured
    override val webClientId: String = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        .let { if (it == 0) "" else context.getString(it) }
    override suspend fun authenticate(token: String) = source.loginWithGoogleToken(token)
    override fun signOut() = source.logout()
}

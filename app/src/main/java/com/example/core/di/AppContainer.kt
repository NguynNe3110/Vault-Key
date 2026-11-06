package com.example.core.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.remote.datasource.FirebaseDataSource
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.VaultRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.VaultRepository
import com.example.domain.usecase.SaveRecordUseCase
import com.example.feature.auth.login.LoginViewModel
import com.example.feature.editor.EditorViewModel
import com.example.feature.home.HomeViewModel
import com.example.feature.main.MainViewModel
import com.example.feature.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firebase = FirebaseDataSource(context.applicationContext)
    private val auth: AuthRepository = AuthRepositoryImpl(context.applicationContext, firebase)
    private val vault = VaultRepositoryImpl(context.applicationContext, auth, firebase, appScope)
    val viewModelFactory: ViewModelProvider.Factory = VaultViewModelFactory(auth, vault)
    fun close() { appScope.cancel(); vault.close(); firebase.dispose() }
}

class VaultViewModelFactory(private val auth: AuthRepository, private val vault: VaultRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when(modelClass) {
        MainViewModel::class.java -> MainViewModel(vault, auth)
        LoginViewModel::class.java -> LoginViewModel(auth)
        HomeViewModel::class.java -> HomeViewModel(vault, auth)
        EditorViewModel::class.java -> EditorViewModel(vault, SaveRecordUseCase(vault))
        SettingsViewModel::class.java -> SettingsViewModel(vault, auth)
        else -> throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
    } as T
}

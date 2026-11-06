package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

private fun String.searchKey() = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace('đ', 'd')

class HomeViewModel(private val vault: VaultRepository, auth: AuthRepository) : ViewModel() {
    private val filters = MutableStateFlow(HomeUiState())
    val state = combine(vault.contents, auth.currentUser, filters) { data, user, input ->
        val base = data.accounts.filter { when(input.tab) { 1 -> it.kind in listOf("CARD", "BANK"); 2 -> it.isFavorite; else -> true } }
        val records = base.filter { item ->
            (input.category == "Tất cả" || item.category == input.category) && (input.kind == "ALL" || item.kind == input.kind) &&
                (input.query.isBlank() || listOf(item.title, item.username, item.category, item.maskedIdentifier, item.customFields.values.joinToString(" "))
                    .joinToString(" ").searchKey().contains(input.query.trim().searchKey()))
        }.sortedWith(compareByDescending<AccountItem> { it.isFavorite }.thenByDescending { it.updatedAt })
        input.copy(records = records, levels = data.levels, total = base.size,
            categories = base.map { it.category }.filter { it.isNotBlank() }.distinct(),
            counts = base.groupingBy { it.kind }.eachCount(), displayName = user?.displayName)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
    private val channel = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    fun onEvent(event: HomeUiEvent) {
        when(event) {
            is HomeUiEvent.TabChanged -> filters.update { it.copy(tab = event.tab) }
            is HomeUiEvent.QueryChanged -> filters.update { it.copy(query = event.value) }
            is HomeUiEvent.CategoryChanged -> filters.update { it.copy(category = event.value) }
            is HomeUiEvent.KindChanged -> filters.update { it.copy(kind = event.value) }
            HomeUiEvent.ClearFilters -> filters.update { it.copy(query = "", category = "Tất cả", kind = "ALL") }
            is HomeUiEvent.FavoriteClicked -> viewModelScope.launch {
                try { vault.save(event.account.copy(isFavorite = !event.account.isFavorite, updatedAt = System.currentTimeMillis())) }
                catch(e: CancellationException) { throw e }
                catch(e: Exception) { channel.send(HomeUiEffect.Message("Chưa thay đổi được mục yêu thích.")) }
            }
        }
    }
}

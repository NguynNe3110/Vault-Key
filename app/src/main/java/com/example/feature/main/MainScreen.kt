package com.example.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.CollectEffects
import com.example.ui.component.navigation.VaultNavigation
import com.example.ui.component.surface.VaultMessage
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel, tab: Int?, editor: Boolean, showLoading: Boolean,
    onSelectTab: (Int) -> Unit, content: @Composable (MainUiState, (String) -> Unit) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    CollectEffects(viewModel.effects) { if (it is MainUiEffect.Message) message(it.value) }
    MainContent(state, tab, editor, showLoading, snackbar, onSelectTab) { content(state, message) }
}

@Composable
fun MainContent(state: MainUiState, tab: Int?, editor: Boolean, showLoading: Boolean,
    snackbar: SnackbarHostState, onSelectTab: (Int) -> Unit, content: @Composable () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar, Modifier.imePadding().padding(bottom = if (editor) 84.dp else 12.dp)) { data -> VaultMessage(data.visuals.message, data::dismiss) } },
        bottomBar = {
            if (tab != null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.widthIn(max = 740.dp)) { VaultNavigation(tab, onSelectTab) }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (showLoading && state.error != null) Text(state.error, Modifier.padding(24.dp).align(Alignment.Center), style = MaterialTheme.typography.bodyLarge)
            else if (showLoading && !state.ready) CircularProgressIndicator(Modifier.size(28.dp).align(Alignment.Center), strokeWidth = 2.dp)
            else content()
        }
    }
}

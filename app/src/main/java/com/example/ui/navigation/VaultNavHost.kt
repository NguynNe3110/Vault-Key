package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.core.ui.GoogleAccountPicker
import com.example.core.ui.OnForeground
import com.example.domain.model.RecordKind
import com.example.feature.auth.login.LoginScreen
import com.example.feature.auth.login.LoginViewModel
import com.example.feature.detail.DetailScreen
import com.example.feature.editor.EditorScreen
import com.example.feature.editor.EditorViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import com.example.feature.main.*
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun VaultNavHost(factory: ViewModelProvider.Factory) {
    val main: MainViewModel = viewModel(factory = factory)
    val state by main.state.collectAsStateWithLifecycle()
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.LOGIN
    var localAccess by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun entryRoute(target: String) {
        controller.navigate(target) { popUpTo(controller.graph.id) { inclusive = false }; launchSingleTop = true }
    }
    LaunchedEffect(state.user?.uid) { detailId = null }
    LaunchedEffect(state.user?.uid, localAccess, entry?.id) {
        val currentRoute = controller.currentDestination?.route ?: return@LaunchedEffect
        detailId = null
        if (state.user == null && !localAccess && currentRoute != Routes.LOGIN) entryRoute(Routes.LOGIN)
        else if ((state.user != null || localAccess) && currentRoute == Routes.LOGIN) entryRoute(Routes.HOME)
    }
    OnForeground { main.onEvent(MainUiEvent.Foreground) }
    val editor = route.startsWith("editor/")
    val tab = Routes.tabs.indexOf(route).takeIf { it >= 0 && (state.user != null || localAccess) }
    fun edit(id: String?, kind: RecordKind) { detailId = null; controller.navigate("editor/${kind.name}?id=${Uri.encode(id.orEmpty())}") }
    MainScreen(main, tab, editor, route != Routes.LOGIN, onSelectTab = { index ->
        controller.navigate(Routes.tabs[index]) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }
    }) { _, message ->
        NavHost(controller, startDestination = Routes.LOGIN) {
            composable(Routes.LOGIN) {
                val login: LoginViewModel = viewModel(factory = factory)
                LoginScreen(login) { localAccess = true }
            }
            Routes.tabs.take(3).forEachIndexed { index, destination ->
                composable(destination) {
                    val home: HomeViewModel = viewModel(factory = factory)
                    HomeScreen(home, index,
                        onAdd = { edit(null, if (index == 1) RecordKind.CARD else RecordKind.ACCOUNT) },
                        onDetail = { detailId = it.id }, onSettings = { controller.navigate(Routes.SETTINGS) { launchSingleTop = true } }, onMessage = message)
                }
            }
            composable(Routes.SETTINGS) {
                val settings: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(settings, onSignIn = { localAccess = false }, onSignOut = {
                    scope.launch {
                        try { GoogleAccountPicker.clear(context) }
                        catch(e: CancellationException) { throw e }
                        catch(e: Exception) { /* Firebase sign-out still clears the app session. */ }
                        localAccess = false
                        main.onEvent(MainUiEvent.SignOut)
                    }
                }, onMessage = message)
            }
            composable(Routes.EDITOR, arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType; defaultValue = "" })) { backStack ->
                val editorVm: EditorViewModel = viewModel(factory = factory)
                EditorScreen(editorVm, backStack.arguments?.getString("id")?.ifBlank { null },
                    RecordKind.from(backStack.arguments?.getString("kind").orEmpty()), onClose = { controller.popBackStack() })
            }
        }
    }
    state.contents.accounts.find { it.id == detailId }?.let { record ->
        DetailScreen(record, state.contents.levels.find { it.id == record.securityLevelId },
            onDismiss = { detailId = null }, onEdit = { edit(record.id, RecordKind.from(record.kind)) },
            onDelete = { main.onEvent(MainUiEvent.DeleteRecord(record.id)); detailId = null })
    }
}

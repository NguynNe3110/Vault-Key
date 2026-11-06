package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import com.example.domain.model.*
import com.example.feature.auth.login.*
import com.example.feature.home.*
import com.example.feature.main.*
import com.example.feature.settings.*
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = "w375dp-h812dp-mdpi", sdk = [36])
class GreetingScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private val repo = TestVaultRepository().apply { contents.value = contents.value.copy(accounts = listOf(
        AccountItem("1", "Visa chi tiêu", "Tài chính", "NGUYEN MINH AN", 1, kind = "CARD", maskedIdentifier = "**** **** **** 4826", isFavorite = true),
        AccountItem("2", "Gmail cá nhân", "Cá nhân", "minhan@gmail.com", 2),
        AccountItem("3", "Thông tin bảo hiểm", "Cá nhân", "", 1, kind = "NOTE")
    )) }
    private fun home(dark: Boolean = false, fontScale: Float = 1f): HomeViewModel {
        val vm = HomeViewModel(repo, TestAuthRepository())
        store.put("home", vm)
        compose.setContent { MyApplicationTheme(darkTheme = dark) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                MainContent(MainUiState(ready = true), 0, false, false, remember { androidx.compose.material3.SnackbarHostState() }, {}) {
                    HomeScreen(vm, 0, {}, {}, {}, {})
                }
            }
        } }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); vm.state.value.records.size == 3 }
        compose.waitForIdle()
        return vm
    }
    @After fun cleanup() { store.clear() }
    @Test fun homeLight() {
        home()
        compose.onNodeWithText("Kho lưu trữ").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/screenshots/home-light.png")
    }
    @Test fun homeDark() { home(dark = true); compose.onRoot().captureRoboImage("build/reports/screenshots/home-dark.png") }
    @Test fun searchIgnoresVietnameseAccents() {
        val vm = home()
        compose.onNodeWithTag("vault_search").performTextInput("ca nhan")
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); vm.state.value.records.none { it.kind == "CARD" } }
        compose.onNodeWithTag("vault_list").performScrollToNode(hasText("Gmail cá nhân"))
        compose.onNodeWithText("Gmail cá nhân").assertIsDisplayed()
        compose.onAllNodesWithText("Visa chi tiêu").assertCountEquals(0)
        compose.onRoot().captureRoboImage("build/reports/screenshots/search.png")
    }
    @Test fun loginLargeTextRemainsScrollable() {
        compose.setContent { MyApplicationTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { LoginContent(LoginUiState(available = true), {}, {}) }
            }
        } }
        compose.onNodeWithTag("google_login_button").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/screenshots/login-large-text.png")
    }
    @Test fun loginLight() {
        compose.setContent { MyApplicationTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { LoginContent(LoginUiState(available = true), {}, {}) } } }
        compose.onRoot().captureRoboImage("build/reports/screenshots/login-light.png")
    }
    @Test @Config(application = Application::class, qualifiers = "w960dp-h600dp-land-mdpi", sdk = [36])
    fun tabletLandscape() { home(); compose.onRoot().captureRoboImage("build/reports/screenshots/home-tablet.png") }
    @Test fun homeLargeText() {
        home(fontScale = 2f)
        compose.onNodeWithTag("vault_list").performScrollToNode(hasText("Gmail cá nhân"))
        compose.onNodeWithText("Gmail cá nhân").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/screenshots/home-large-text.png")
    }
    @Test fun settingsAvoidsTechnicalAndRoutineSyncMessages() {
        compose.setContent { MyApplicationTheme {
            MainContent(MainUiState(ready = true), 3, false, false, remember { androidx.compose.material3.SnackbarHostState() }, {}) {
                SettingsContent(SettingsUiState(contents = repo.contents.value, categories = VaultDefaults.categories,
                    user = UserSession("test", "minhan@gmail.com", "Minh An"), cloud = CloudState.Current), {}, {})
            }
        } }
        compose.onAllNodesWithText("Tự động đồng bộ", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Firebase", substring = true).assertCountEquals(0)
        compose.onRoot().captureRoboImage("build/reports/screenshots/settings-light.png")
    }
    @Test fun settingsErrorOnlyShowsActionableCopy() {
        compose.setContent { MyApplicationTheme(darkTheme = true) {
            MainContent(MainUiState(ready = true), 3, false, false, remember { androidx.compose.material3.SnackbarHostState() }, {}) {
                SettingsContent(SettingsUiState(contents = repo.contents.value, categories = VaultDefaults.categories,
                    user = UserSession("test", "minhan@gmail.com", "Minh An"), cloud = CloudState.Attention), {}, {})
            }
        } }
        compose.onNodeWithText("Thử lại").assertIsDisplayed()
        compose.onAllNodesWithText("PERMISSION_DENIED", substring = true).assertCountEquals(0)
        compose.onRoot().captureRoboImage("build/reports/screenshots/settings-error-dark.png")
    }
}

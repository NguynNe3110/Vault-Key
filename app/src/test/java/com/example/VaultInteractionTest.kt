package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.core.di.VaultViewModelFactory
import com.example.ui.navigation.VaultNavHost
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, qualifiers = "w375dp-h812dp-mdpi", sdk = [36])
class VaultInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val repository = TestVaultRepository()
    private val factory = VaultViewModelFactory(TestAuthRepository(), repository)
    private fun await(condition: () -> Boolean) {
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); condition() }
        compose.waitForIdle()
    }
    private fun openVault() {
        compose.setContent { MyApplicationTheme { VaultNavHost(factory) } }
        compose.onNodeWithText("Dùng trên thiết bị này").performScrollTo().performClick()
        compose.onNodeWithTag("add_record").assertExists()
    }
    @Test fun createEditFavoriteAndDeleteCard() {
        openVault()
        compose.onNodeWithTag("add_record").performClick()
        compose.onNodeWithText("Thẻ ngân hàng", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("record_title").performTextInput("Visa thử nghiệm")
        compose.onNodeWithTag("record_number").performScrollTo().performTextInput("123456")
        compose.onNodeWithTag("mask_preview").assertTextEquals("**3456")
        compose.onRoot().captureRoboImage("build/reports/screenshots/editor-card.png")
        compose.onNodeWithTag("save_record").performClick()
        await { repository.contents.value.accounts.isNotEmpty() }
        var card = repository.contents.value.accounts.single()
        assertEquals("**3456", card.maskedIdentifier)
        assertFalse(card.customFields.values.contains("123456"))
        compose.onNodeWithTag("vault_list").performScrollToNode(hasText("Visa thử nghiệm"))
        compose.onNodeWithContentDescription("Yêu thích Visa thử nghiệm").performClick()
        await { repository.contents.value.accounts.single().isFavorite }
        compose.onNodeWithText("Visa thử nghiệm").performClick()
        compose.onNode(isDialog()).captureRoboImage("build/reports/screenshots/detail-card.png")
        compose.onNodeWithText("Chỉnh sửa thông tin").performScrollTo().performClick()
        compose.onNodeWithTag("record_title").performTextReplacement("Visa đã sửa")
        compose.onNodeWithTag("save_record").performClick()
        await { repository.contents.value.accounts.single().title == "Visa đã sửa" }
        card = repository.contents.value.accounts.single()
        assertEquals("**3456", card.maskedIdentifier)
        assertTrue(card.isFavorite)
        compose.onNodeWithTag("vault_list").performScrollToNode(hasText("Visa đã sửa"))
        compose.onNodeWithText("Visa đã sửa").performClick()
        compose.onNodeWithText("Xóa mục này").performScrollTo().performClick()
        compose.onNodeWithText("Xóa", substring = false).performClick()
        await { repository.contents.value.accounts.isEmpty() }
    }
    @Test fun fullAppLargeTextNavigation() {
        RuntimeEnvironment.setFontScale(2f)
        compose.setContent { MyApplicationTheme(darkTheme = true) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) { VaultNavHost(factory) }
        } }
        compose.onNodeWithText("Dùng trên thiết bị này").performScrollTo().performClick()
        compose.onNodeWithText("Cài đặt").performClick()
        compose.onNodeWithText("Quy ước mật khẩu").assertExists()
        compose.onRoot().captureRoboImage("build/reports/screenshots/app-large-text-dark.png")
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("Che vị trí 1 và 5"))
        compose.onNodeWithText("Che vị trí 1 và 5").performClick()
        compose.onNodeWithText("Lưu quy tắc").assertIsDisplayed()
        compose.onNodeWithText("Số mẫu").performScrollTo().assertIsDisplayed()
        compose.onNode(isDialog()).captureRoboImage("build/reports/screenshots/rule-dialog-large-dark.png")
    }
    @Test fun invalidFormKeepsInputAndShowsInlineError() {
        openVault()
        compose.onNodeWithTag("add_record").performClick()
        compose.onNodeWithTag("save_record").performClick()
        compose.onNodeWithText("Nhập tên mục.").assertIsDisplayed()
        assertTrue(repository.contents.value.accounts.isEmpty())
        compose.onNodeWithTag("record_title").performTextInput("Tài khoản mới")
        compose.onNodeWithTag("save_record").performClick()
        await { repository.contents.value.accounts.singleOrNull()?.title == "Tài khoản mới" }
    }
    @Test fun maskingRuleUsesCustomDialogAndLivePreview() {
        openVault()
        compose.onNodeWithText("Cài đặt").performClick()
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("Che vị trí 1 và 5"))
        compose.onNodeWithText("Che vị trí 1 và 5").performClick()
        compose.onNode(hasText("*234%6") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("Vị trí và ký tự thay thế").assertExists()
        compose.onNode(isDialog()).captureRoboImage("build/reports/screenshots/rule-dialog.png")
        compose.onNodeWithContentDescription("Vị trí và ký tự thay thế").performTextReplacement("1-3=#")
        compose.onNode(hasText("###456") and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }
}

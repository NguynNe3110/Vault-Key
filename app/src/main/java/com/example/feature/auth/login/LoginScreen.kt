package com.example.feature.auth.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.ui.GoogleAccountPicker
import com.example.feature.auth.login.component.LoginIllustration
import com.example.ui.component.brand.BrandMark
import com.example.ui.component.button.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLocal: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LoginContent(state, onLocal, onGoogle = {
        scope.launch {
            viewModel.onEvent(LoginUiEvent.Start)
            try { viewModel.onEvent(LoginUiEvent.TokenReceived(GoogleAccountPicker.request(context, state.clientId))) }
            catch(e: GetCredentialCancellationException) { viewModel.onEvent(LoginUiEvent.Cancel) }
            catch(e: NoCredentialException) { viewModel.onEvent(LoginUiEvent.Failed(noAccount = true)) }
            catch(e: CancellationException) { viewModel.onEvent(LoginUiEvent.Cancel); throw e }
            catch(e: Exception) { viewModel.onEvent(LoginUiEvent.Failed()) }
        }
    })
}

@Composable
fun LoginContent(state: LoginUiState, onLocal: () -> Unit, onGoogle: () -> Unit) {
    Column(Modifier.widthIn(max = 520.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        BrandMark()
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kho riêng,\ndễ tìm lại.", style = MaterialTheme.typography.displaySmall)
            Text("Tài khoản, thẻ và ghi chú của bạn.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LoginIllustration()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.error != null) Text(state.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            VaultButton(if (state.busy) "Đang kết nối…" else "Tiếp tục với Google", onGoogle,
                Modifier.fillMaxWidth().testTag("google_login_button"), tone = ButtonTone.Secondary,
                enabled = state.available && !state.busy, busy = state.busy,
                leading = { Icon(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp), tint = Color.Unspecified) })
            VaultButton("Dùng trên thiết bị này", onLocal, Modifier.fillMaxWidth(), tone = ButtonTone.Quiet, enabled = !state.busy)
            if (!state.available) Text("Đăng nhập hiện chưa sẵn sàng.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

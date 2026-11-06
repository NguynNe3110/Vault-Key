package com.example.core.ui

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Adapts the system account picker; screens never access Firebase or token storage. */
object GoogleAccountPicker {
    suspend fun request(context: Context, clientId: String): String {
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = result.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    suspend fun clear(context: Context) { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
}

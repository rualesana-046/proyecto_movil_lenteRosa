package com.lenterosa.app.account

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lenterosa.app.R
import javax.inject.Inject

/** Obtiene un ID token de Google para iniciar sesión o reautenticarse en Firebase. */
fun interface GoogleIdTokenProvider {

    /** `activityContext` debe ser una Activity: Credential Manager muestra su selector sobre ella. */
    suspend fun requestIdToken(activityContext: Context): String
}

class CredentialManagerGoogleIdTokenProvider @Inject constructor() : GoogleIdTokenProvider {

    override suspend fun requestIdToken(activityContext: Context): String {
        // El ID de cliente web lo genera el plugin de google-services a partir de google-services.json.
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthException(AuthFailure.CANCELLED, e)
        } catch (e: NoCredentialException) {
            throw AuthException(AuthFailure.NO_GOOGLE_ACCOUNT, e)
        } catch (e: GetCredentialException) {
            throw AuthException(AuthFailure.UNKNOWN, e)
        }
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw AuthException(AuthFailure.UNKNOWN)
    }
}

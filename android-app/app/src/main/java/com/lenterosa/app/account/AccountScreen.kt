package com.lenterosa.app.account

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import kotlinx.coroutines.launch

/** Pantalla de cuenta (RF-02): iniciar sesión, verificar el correo, apodo, cerrar sesión y eliminar la cuenta. */
@Composable
fun AccountRoute(
    viewModel: AccountViewModel,
    requestGoogleIdToken: suspend () -> String,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Pide la credencial de Google y la entrega al ViewModel; cancelar no muestra error.
    val withGoogle: (suspend (String) -> Unit) -> Unit = { onToken ->
        scope.launch {
            try {
                onToken(requestGoogleIdToken())
            } catch (e: AuthException) {
                viewModel.reportFailure(e.failure)
            }
        }
    }
    AccountScreen(
        state = state,
        onSignIn = viewModel::signIn,
        onSignUp = viewModel::signUp,
        onGoogle = { withGoogle { viewModel.signInWithGoogle(it) } },
        onResendVerification = viewModel::resendVerification,
        onCheckVerification = viewModel::checkVerification,
        onSaveNickname = viewModel::saveNickname,
        onSignOut = viewModel::signOut,
        onDeleteWithPassword = { viewModel.deleteAccount(Reauthentication.WithPassword(it)) },
        onDeleteWithGoogle = { withGoogle { token -> viewModel.deleteAccount(Reauthentication.WithGoogle(token)) } },
        modifier = modifier,
    )
}

@Composable
fun AccountScreen(
    state: AccountUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onGoogle: () -> Unit,
    onResendVerification: () -> Unit,
    onCheckVerification: () -> Unit,
    onSaveNickname: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteWithPassword: (String) -> Unit,
    onDeleteWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.headlineMedium)
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.account_busy), style = MaterialTheme.typography.bodySmall)
        }
        state.message?.let { MessageCard(it) }

        val session = state.session
        when {
            session == null -> SignedOut(state.busy, onSignIn, onSignUp, onGoogle)
            state.needsEmailVerification -> VerifyEmail(session, state.busy, onResendVerification, onCheckVerification, onSignOut)
            else -> SignedIn(session, state.busy, onSaveNickname, onSignOut, onDeleteWithPassword, onDeleteWithGoogle)
        }
    }
}

@Composable
private fun ColumnScope.SignedOut(
    busy: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onGoogle: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }

    Text(stringResource(R.string.account_guest_hint), style = MaterialTheme.typography.bodyMedium)
    PrimaryTabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.account_tab_sign_in)) })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.account_tab_sign_up)) })
    }
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.account_email)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(stringResource(R.string.account_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    if (tab == 1) {
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it.take(AccountViewModel.MAX_NICKNAME_LENGTH) },
            label = { Text(stringResource(R.string.account_nickname)) },
            supportingText = { Text(stringResource(R.string.account_nickname_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val canSubmit = !busy && email.isNotBlank() && password.isNotEmpty()
    Button(
        onClick = { if (tab == 0) onSignIn(email, password) else onSignUp(email, password, nickname) },
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(if (tab == 0) R.string.account_sign_in else R.string.account_sign_up))
    }
    Text(
        stringResource(R.string.account_or),
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButton(onClick = onGoogle, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.account_google))
    }
}

@Composable
private fun ColumnScope.VerifyEmail(
    session: Session,
    busy: Boolean,
    onResend: () -> Unit,
    onCheck: () -> Unit,
    onSignOut: () -> Unit,
) {
    Text(stringResource(R.string.account_verify_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.account_verify_body, session.email.orEmpty()), style = MaterialTheme.typography.bodyMedium)
    Button(onClick = onCheck, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.account_verify_check))
    }
    OutlinedButton(onClick = onResend, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.account_verify_resend))
    }
    TextButton(onClick = onSignOut, enabled = !busy) { Text(stringResource(R.string.account_sign_out)) }
}

@Composable
private fun ColumnScope.SignedIn(
    session: Session,
    busy: Boolean,
    onSaveNickname: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteWithPassword: (String) -> Unit,
    onDeleteWithGoogle: () -> Unit,
) {
    var nickname by rememberSaveable(session.uid) { mutableStateOf(session.nickname.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Text(
        session.nickname?.let { stringResource(R.string.account_hello, it) } ?: stringResource(R.string.account_hello_anonymous),
        style = MaterialTheme.typography.titleLarge,
    )
    OutlinedTextField(
        value = nickname,
        onValueChange = { nickname = it.take(AccountViewModel.MAX_NICKNAME_LENGTH) },
        label = { Text(stringResource(R.string.account_nickname)) },
        supportingText = { Text(stringResource(R.string.account_nickname_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { onSaveNickname(nickname) }, enabled = !busy) { Text(stringResource(R.string.account_save_nickname)) }
        OutlinedButton(onClick = onSignOut, enabled = !busy) { Text(stringResource(R.string.account_sign_out)) }
    }
    TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
        Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
    }

    if (confirmDelete) {
        DeleteAccountDialog(
            usesPassword = session.usesPassword,
            onDismiss = { confirmDelete = false },
            onConfirmPassword = { confirmDelete = false; onDeleteWithPassword(it) },
            onConfirmGoogle = { confirmDelete = false; onDeleteWithGoogle() },
        )
    }
}

// Reautenticación obligatoria antes de eliminar (ADR 0006).
@Composable
private fun DeleteAccountDialog(
    usesPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirmPassword: (String) -> Unit,
    onConfirmGoogle: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.account_delete_body))
                if (usesPassword) {
                    Text(stringResource(R.string.account_delete_reauth_password))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.account_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                } else {
                    Text(stringResource(R.string.account_delete_reauth_google))
                }
            }
        },
        confirmButton = {
            if (usesPassword) {
                TextButton(onClick = { onConfirmPassword(password) }, enabled = password.isNotEmpty()) {
                    Text(stringResource(R.string.account_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onConfirmGoogle) {
                    Text(stringResource(R.string.account_delete_confirm_google), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.account_cancel)) } },
    )
}

@Composable
private fun MessageCard(message: AccountMessage) {
    val colors = if (message.isError) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(stringResource(message.text()), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@StringRes
private fun AccountMessage.text(): Int = when (this) {
    AccountMessage.VERIFICATION_SENT -> R.string.account_message_verification_sent
    AccountMessage.NOT_VERIFIED_YET -> R.string.account_message_not_verified_yet
    AccountMessage.NICKNAME_SAVED -> R.string.account_message_nickname_saved
    AccountMessage.ACCOUNT_DELETED -> R.string.account_message_account_deleted
    AccountMessage.API_UNAVAILABLE -> R.string.account_message_api_unavailable
    AccountMessage.INVALID_CREDENTIALS -> R.string.account_message_invalid_credentials
    AccountMessage.EMAIL_IN_USE -> R.string.account_message_email_in_use
    AccountMessage.WEAK_PASSWORD -> R.string.account_message_weak_password
    AccountMessage.INVALID_EMAIL -> R.string.account_message_invalid_email
    AccountMessage.NETWORK -> R.string.account_message_network
    AccountMessage.NO_GOOGLE_ACCOUNT -> R.string.account_message_no_google_account
    AccountMessage.SERVER -> R.string.account_message_server
    AccountMessage.UNKNOWN -> R.string.account_message_unknown
}

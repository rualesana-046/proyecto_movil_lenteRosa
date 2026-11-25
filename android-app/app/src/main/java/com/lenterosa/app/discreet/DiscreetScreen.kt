package com.lenterosa.app.discreet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lenterosa.app.R

/**
 * Colores del Modo discreto: grises neutros, sin el rosa de la marca, sin vidrio ni animaciones.
 * DiscreetColorsTest verifica el contraste AA.
 */
data class DiscreetColors(val background: Color, val button: Color, val text: Color, val secondaryText: Color) {
    companion object {
        val Light = DiscreetColors(Color(0xFFE6E6E6), Color(0xFFFFFFFF), Color(0xFF1C1C1C), Color(0xFF4A4A4A))
        val Dark = DiscreetColors(Color(0xFF1A1A1A), Color(0xFF2E2E2E), Color(0xFFEDEDED), Color(0xFFBDBDBD))
    }
}

object DiscreetTags {
    const val SEND = "discreet_send"
    const val FAKE_CALL = "discreet_fake_call"
    const val MARK = "discreet_mark"
    const val UNDO = "discreet_undo"
    const val STATUS = "discreet_status"
}

/** Pantalla gris con las tres acciones de un toque. */
@Composable
fun DiscreetScreen(
    state: DiscreetUiState,
    colors: DiscreetColors,
    onSendLocation: () -> Unit,
    onFakeCall: () -> Unit,
    onMark: () -> Unit,
    onUndo: () -> Unit,
    onDismissStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onCallEmergency: () -> Unit,
    onExit: () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides colors.text) {
        Box(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onExit) { Text(stringResource(R.string.discreet_exit), color = colors.text) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.discreet_settings))
                    }
                }
                Spacer(Modifier.weight(0.3f))
                ActionButton(R.drawable.ic_send_location, stringResource(R.string.discreet_send), sendSubtitle(state), colors,
                    DiscreetTags.SEND, onSendLocation, busy = state.preparingLocation)
                ActionButton(R.drawable.ic_phone, stringResource(R.string.discreet_fake_call),
                    stringResource(R.string.discreet_fake_call_hint, state.settings.fakeCallName, delayText(state.settings.fakeCallDelaySeconds)),
                    colors, DiscreetTags.FAKE_CALL, onFakeCall)
                ActionButton(R.drawable.ic_flag, stringResource(R.string.discreet_mark), stringResource(R.string.discreet_mark_hint),
                    colors, DiscreetTags.MARK, onMark)
                Spacer(Modifier.weight(1f))
                state.sendStatus?.let { StatusText(it, colors, onDismissStatus) }
                (state.call as? FakeCall.Scheduled)?.let {
                    Text(stringResource(R.string.discreet_call_in, it.secondsLeft), color = colors.secondaryText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                TextButton(onClick = onCallEmergency, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.discreet_emergency), color = colors.text, textAlign = TextAlign.Center)
                }
            }
            state.undo?.let { offer ->
                UndoBar(offer, colors, onUndo, Modifier.align(Alignment.BottomCenter).padding(16.dp))
            }
        }
    }
}

@Composable
private fun sendSubtitle(state: DiscreetUiState): String {
    val contact = state.settings.contacts.firstOrNull()
    return if (contact != null) stringResource(R.string.discreet_send_to, contact.name) else stringResource(R.string.discreet_send_choose)
}

@Composable
private fun delayText(seconds: Int): String =
    if (seconds == 0) stringResource(R.string.discreet_delay_now) else stringResource(R.string.discreet_delay_seconds, seconds)

@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    colors: DiscreetColors,
    tag: String,
    onClick: () -> Unit,
    busy: Boolean = false,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.button, contentColor = colors.text),
        modifier = Modifier.fillMaxWidth().height(96.dp).testTag(tag),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = colors.text, strokeWidth = 2.dp)
                } else {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
            }
        }
    }
}

@Composable
private fun StatusText(status: SendStatus, colors: DiscreetColors, onDismiss: () -> Unit) {
    val text = stringResource(
        when (status) {
            SendStatus.MESSAGE_READY -> R.string.discreet_status_ready
            SendStatus.MESSAGE_READY_APPROXIMATE -> R.string.discreet_status_approximate
            SendStatus.MESSAGE_READY_WITHOUT_LOCATION -> R.string.discreet_status_no_location
            SendStatus.NO_MESSAGING_APP -> R.string.discreet_status_no_app
        },
    )
    Surface(color = colors.button, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag(DiscreetTags.STATUS)) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = colors.text, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.map_ok), color = colors.text) }
        }
    }
}

/** Aviso con "Deshacer" durante 5 segundos. */
@Composable
private fun UndoBar(offer: UndoOffer, colors: DiscreetColors, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val text = when (offer) {
        is UndoOffer.Call -> if (offer.delaySeconds == 0) {
            stringResource(R.string.discreet_undo_call_now)
        } else {
            stringResource(R.string.discreet_undo_call, offer.delaySeconds)
        }
        is UndoOffer.Mark -> stringResource(if (offer.withLocation) R.string.discreet_undo_mark else R.string.discreet_undo_mark_no_location)
    }
    Surface(color = colors.text, contentColor = colors.background, shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f).padding(vertical = 14.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.background)
            TextButton(onClick = onUndo, modifier = Modifier.testTag(DiscreetTags.UNDO)) {
                Text(stringResource(R.string.discreet_undo), color = colors.background)
            }
        }
    }
}

/** Llamada entrante simulada, a pantalla completa. */
@Composable
fun FakeCallScreen(call: FakeCall, onAnswer: () -> Unit, onHangUp: () -> Unit) {
    val caller = when (call) {
        is FakeCall.Ringing -> call.caller
        is FakeCall.InCall -> call.caller
        else -> return
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF101010)).safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Text(caller, style = MaterialTheme.typography.displaySmall, color = Color.White, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        Text(
            if (call is FakeCall.InCall) {
                String.format(java.util.Locale.US, "%02d:%02d", call.seconds / 60, call.seconds % 60)
            } else {
                stringResource(R.string.discreet_incoming_call)
            },
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFD0D0D0),
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RoundCallButton(stringResource(R.string.discreet_hang_up), Color(0xFFC62828), R.drawable.ic_call_end, onHangUp)
            if (call is FakeCall.Ringing) {
                RoundCallButton(stringResource(R.string.discreet_answer), Color(0xFF2E7D32), R.drawable.ic_phone, onAnswer)
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun RoundCallButton(label: String, color: Color, @DrawableRes icon: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(72.dp).background(color, CircleShape)) {
            Icon(painterResource(icon), contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/** Ajustes: contactos de confianza (hasta 3, solo en el teléfono) y la llamada simulada. */
@Composable
fun DiscreetSettingsScreen(
    settings: DiscreetSettings,
    colors: DiscreetColors,
    onSaveFakeCall: (String, Int) -> Unit,
    onAddContact: (String, String) -> Unit,
    onRemoveContact: (TrustedContact) -> Unit,
    onBack: () -> Unit,
) {
    var caller by rememberSaveable(settings.fakeCallName) { mutableStateOf(settings.fakeCallName) }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    CompositionLocalProvider(LocalContentColor provides colors.text) {
        Column(
            Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.navigation_back), color = colors.text) }
            Text(stringResource(R.string.discreet_settings), style = MaterialTheme.typography.headlineSmall, color = colors.text,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.discreet_settings_local), color = colors.secondaryText, style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.discreet_fake_call), style = MaterialTheme.typography.titleMedium, color = colors.text,
                modifier = Modifier.semantics { heading() })
            OutlinedTextField(
                value = caller,
                onValueChange = { caller = it.take(40) },
                label = { Text(stringResource(R.string.discreet_caller_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscreetSettings.DELAY_OPTIONS.forEach { seconds ->
                    FilterChip(
                        selected = settings.fakeCallDelaySeconds == seconds,
                        onClick = { onSaveFakeCall(caller, seconds) },
                        label = { Text(delayText(seconds)) },
                    )
                }
            }
            Button(onClick = { onSaveFakeCall(caller, settings.fakeCallDelaySeconds) }) { Text(stringResource(R.string.discreet_save)) }

            Text(stringResource(R.string.discreet_contacts), style = MaterialTheme.typography.titleMedium, color = colors.text,
                modifier = Modifier.semantics { heading() })
            settings.contacts.forEach { contact ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${contact.name} · ${contact.phone}", color = colors.text, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onRemoveContact(contact) }) { Text(stringResource(R.string.discreet_remove), color = colors.text) }
                }
            }
            if (settings.contacts.size < DiscreetSettings.MAX_CONTACTS) {
                OutlinedTextField(contactName, { contactName = it.take(40) }, label = { Text(stringResource(R.string.discreet_contact_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(contactPhone, { contactPhone = it.take(20) }, label = { Text(stringResource(R.string.discreet_contact_phone)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onAddContact(contactName, contactPhone)
                        contactName = ""
                        contactPhone = ""
                    },
                    enabled = contactPhone.count { it.isDigit() } >= 7,
                ) { Text(stringResource(R.string.discreet_add_contact)) }
            } else {
                Text(stringResource(R.string.discreet_contacts_full), color = colors.secondaryText)
            }
        }
    }
}

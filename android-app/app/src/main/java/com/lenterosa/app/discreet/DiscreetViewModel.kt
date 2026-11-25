package com.lenterosa.app.discreet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Fases de la llamada simulada. No hay ninguna llamada real. */
sealed interface FakeCall {
    data object Idle : FakeCall
    data class Scheduled(val secondsLeft: Int) : FakeCall
    data class Ringing(val caller: String) : FakeCall
    data class InCall(val caller: String, val seconds: Int) : FakeCall
}

/** Aviso con "Deshacer" que dura [DiscreetViewModel.UNDO_WINDOW_MS]. */
sealed interface UndoOffer {
    data class Call(val delaySeconds: Int) : UndoOffer
    data class Mark(val id: String, val withLocation: Boolean) : UndoOffer
}

/** Estado real de "Enviar mi ubicación", en lenguaje cotidiano. */
enum class SendStatus {
    /** Se abrió la app de mensajes con el texto; la usuaria decide si lo envía. */
    MESSAGE_READY,

    /** Igual, pero la ubicación es aproximada porque no hay permiso de ubicación precisa. */
    MESSAGE_READY_APPROXIMATE,

    /** No se pudo obtener la ubicación; el mensaje va sin enlace. */
    MESSAGE_READY_WITHOUT_LOCATION,

    /** No hay ninguna app para enviar mensajes. */
    NO_MESSAGING_APP,
}

data class DiscreetUiState(
    val settings: DiscreetSettings = DiscreetSettings(),
    val call: FakeCall = FakeCall.Idle,
    val undo: UndoOffer? = null,
    val sendStatus: SendStatus? = null,
    val preparingLocation: Boolean = false,
)

/** Mensaje listo para abrir en la app de mensajes. */
data class HelpMessage(val text: String, val contact: TrustedContact?, val status: SendStatus)

/**
 * Modo discreto (RF-08, ADR 0013): tres acciones de un solo toque. Funciona sin cuenta y no envía
 * nada a la API: los contactos y las marcas viven solo en este teléfono.
 */
@HiltViewModel
class DiscreetViewModel @Inject constructor(
    private val store: DiscreetStore,
    private val location: HelpLocation,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscreetUiState())
    val state: StateFlow<DiscreetUiState> = _state.asStateFlow()

    val marks = store.marks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var callJob: Job? = null
    private var undoJob: Job? = null

    init {
        viewModelScope.launch { store.settings.collect { settings -> _state.update { it.copy(settings = settings) } } }
    }

    /** Un toque: usa el nombre y el tiempo configurados. */
    fun startFakeCall() {
        val settings = _state.value.settings
        callJob?.cancel()
        callJob = viewModelScope.launch {
            for (left in settings.fakeCallDelaySeconds downTo 1) {
                _state.update { it.copy(call = FakeCall.Scheduled(left)) }
                delay(1_000)
            }
            _state.update { it.copy(call = FakeCall.Ringing(settings.fakeCallName)) }
        }
        offerUndo(UndoOffer.Call(settings.fakeCallDelaySeconds))
    }

    fun answer() {
        val ringing = _state.value.call as? FakeCall.Ringing ?: return
        callJob?.cancel()
        callJob = viewModelScope.launch {
            var seconds = 0
            while (true) {
                _state.update { it.copy(call = FakeCall.InCall(ringing.caller, seconds)) }
                delay(1_000)
                seconds++
            }
        }
    }

    fun hangUp() {
        callJob?.cancel()
        _state.update { it.copy(call = FakeCall.Idle) }
    }

    /** Un toque: guarda la marca con la ubicación aproximada si ya hay permiso (sin preguntar nada). */
    fun markUncomfortable(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val fix = location.current(MARK_LOCATION_TIMEOUT_MS)
            val mark = when (fix) {
                is HelpFix.Found -> UncomfortableMark(UUID.randomUUID().toString(), nowMillis, round3(fix.lat), round3(fix.lng))
                HelpFix.Unavailable -> UncomfortableMark(UUID.randomUUID().toString(), nowMillis)
            }
            store.addMark(mark)
            offerUndo(UndoOffer.Mark(mark.id, withLocation = mark.lat != null))
        }
    }

    fun undo() {
        when (val offer = _state.value.undo) {
            is UndoOffer.Call -> hangUp()
            is UndoOffer.Mark -> viewModelScope.launch { store.removeMark(offer.id) }
            null -> Unit
        }
        dismissUndo()
    }

    fun dismissUndo() {
        undoJob?.cancel()
        _state.update { it.copy(undo = null) }
    }

    /** Prepara el texto con el enlace al mapa; la pantalla abre la app de mensajes. */
    suspend fun prepareHelpMessage(): HelpMessage {
        _state.update { it.copy(preparingLocation = true, sendStatus = null) }
        val fix = location.current(SEND_LOCATION_TIMEOUT_MS)
        val contact = store.settings.first().contacts.firstOrNull()
        _state.update { it.copy(preparingLocation = false) }
        return buildHelpMessage(fix, contact)
    }

    fun reportSendStatus(status: SendStatus) = _state.update { it.copy(sendStatus = status) }

    fun dismissSendStatus() = _state.update { it.copy(sendStatus = null) }

    fun saveFakeCall(name: String, delaySeconds: Int) = viewModelScope.launch { store.setFakeCall(name, delaySeconds) }

    fun addContact(name: String, phone: String) = viewModelScope.launch {
        store.addContact(TrustedContact(name.trim(), phone.filter { it.isDigit() || it == '+' }))
    }

    fun removeContact(contact: TrustedContact) = viewModelScope.launch { store.removeContact(contact) }

    private fun offerUndo(offer: UndoOffer) {
        undoJob?.cancel()
        _state.update { it.copy(undo = offer) }
        undoJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            _state.update { if (it.undo == offer) it.copy(undo = null) else it }
        }
    }

    companion object {
        const val UNDO_WINDOW_MS = 5_000L
        const val SEND_LOCATION_TIMEOUT_MS = 4_000L
        const val MARK_LOCATION_TIMEOUT_MS = 2_000L

        /** La marca guarda la ubicación con 3 decimales (unos 100 m): basta para recordarla. */
        private fun round3(value: Double) = Math.round(value * 1_000) / 1_000.0

        /** Texto del mensaje de ayuda con un enlace a OpenStreetMap (sin depender de Google). */
        fun buildHelpMessage(fix: HelpFix, contact: TrustedContact?): HelpMessage = when (fix) {
            is HelpFix.Found -> {
                val lat = String.format(Locale.US, "%.5f", fix.lat)
                val lng = String.format(Locale.US, "%.5f", fix.lng)
                val link = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=18/$lat/$lng"
                val note = if (fix.precise) "" else " (ubicación aproximada)"
                HelpMessage(
                    text = "Necesito ayuda. Estoy aquí$note: $link",
                    contact = contact,
                    status = if (fix.precise) SendStatus.MESSAGE_READY else SendStatus.MESSAGE_READY_APPROXIMATE,
                )
            }
            HelpFix.Unavailable -> HelpMessage(
                text = "Necesito ayuda. No pude compartir mi ubicación; llámame, por favor.",
                contact = contact,
                status = SendStatus.MESSAGE_READY_WITHOUT_LOCATION,
            )
        }
    }
}

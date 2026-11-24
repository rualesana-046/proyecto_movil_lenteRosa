package com.lenterosa.app.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VisitPeriod { DAY, NIGHT }

/** Preguntas del formulario Aportar (RF-04). */
enum class Question { LIGHTING, WOMEN_PRESENCE, COMMERCIAL_VISIBILITY, VISIT_PERIOD, NIGHT_EXPERIENCE }

/** Mensaje después de enviar; los textos de la API ya vienen en español. */
sealed interface ContributeMessage {
    data object NotSignedIn : ContributeMessage
    data object EmailNotVerified : ContributeMessage
    data class Rejected(val apiMessage: String?, val contactData: Boolean) : ContributeMessage
    data class RateLimited(val apiMessage: String?, val wait: WaitTime?) : ContributeMessage
    data class Failed(val failure: LoadFailure) : ContributeMessage
}

data class ContributeUiState(
    val lighting: Int? = null,
    val womenPresence: Int? = null,
    val commercialVisibility: Int? = null,
    val visitPeriod: VisitPeriod? = null,
    val nightExperience: Int? = null,
    /** Solo de día: eligió "No fui de noche" y la experiencia nocturna se envía sin valor. */
    val notAtNight: Boolean = false,
    val comment: String = "",
    val showMissing: Boolean = false,
    val sending: Boolean = false,
    val message: ContributeMessage? = null,
    val published: Boolean = false,
) {
    /** Preguntas obligatorias sin responder, en el orden del formulario. */
    val missing: List<Question>
        get() = buildList {
            if (lighting == null) add(Question.LIGHTING)
            if (womenPresence == null) add(Question.WOMEN_PRESENCE)
            if (commercialVisibility == null) add(Question.COMMERCIAL_VISIBILITY)
            if (visitPeriod == null) add(Question.VISIT_PERIOD)
            if (nightExperience == null && !(visitPeriod == VisitPeriod.DAY && notAtNight)) add(Question.NIGHT_EXPERIENCE)
        }

    val commentTooLong: Boolean get() = comment.trim().length > MAX_COMMENT_LENGTH

    companion object {
        const val MAX_COMMENT_LENGTH = 500
    }
}

/** Formulario Aportar. La validación definitiva (y la de datos de contacto) la hace la API. */
@HiltViewModel
class ContributeViewModel @Inject constructor(private val repository: PlacesRepository) : ViewModel() {

    private val _state = MutableStateFlow(ContributeUiState())
    val state: StateFlow<ContributeUiState> = _state.asStateFlow()

    fun setRating(question: Question, value: Int) = _state.update {
        when (question) {
            Question.LIGHTING -> it.copy(lighting = value)
            Question.WOMEN_PRESENCE -> it.copy(womenPresence = value)
            Question.COMMERCIAL_VISIBILITY -> it.copy(commercialVisibility = value)
            Question.NIGHT_EXPERIENCE -> it.copy(nightExperience = value, notAtNight = false)
            Question.VISIT_PERIOD -> it
        }.copy(message = null)
    }

    // De noche, "No fui de noche" no tiene sentido: se descarta si estaba elegido.
    fun setVisitPeriod(period: VisitPeriod) = _state.update {
        it.copy(visitPeriod = period, notAtNight = it.notAtNight && period == VisitPeriod.DAY, message = null)
    }

    fun chooseNotAtNight() = _state.update { it.copy(notAtNight = true, nightExperience = null, message = null) }

    fun setComment(comment: String) = _state.update { it.copy(comment = comment, message = null) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** Después de publicar, el formulario vuelve a quedar vacío para la próxima vez. */
    fun onPublishedHandled() {
        _state.value = ContributeUiState()
    }

    fun submit(placeId: String) {
        val current = _state.value
        if (current.sending) return
        if (current.missing.isNotEmpty() || current.commentTooLong) {
            _state.update { it.copy(showMissing = true) }
            return
        }
        val period = current.visitPeriod ?: return
        val draft = ReviewDraft(
            lighting = current.lighting ?: return,
            womenPresence = current.womenPresence ?: return,
            commercialVisibility = current.commercialVisibility ?: return,
            nightExperience = if (period == VisitPeriod.DAY && current.notAtNight) null else current.nightExperience,
            visitPeriod = period.name,
            comment = current.comment.trim().takeIf { it.isNotEmpty() },
        )
        _state.update { it.copy(sending = true, message = null) }
        viewModelScope.launch {
            val result = repository.contribute(placeId, draft)
            _state.update {
                when (result) {
                    is ContributionResult.Published -> it.copy(sending = false, published = true)
                    else -> it.copy(sending = false, message = result.toMessage())
                }
            }
        }
    }

    private fun ContributionResult.toMessage(): ContributeMessage = when (this) {
        ContributionResult.NotSignedIn -> ContributeMessage.NotSignedIn
        ContributionResult.EmailNotVerified -> ContributeMessage.EmailNotVerified
        is ContributionResult.Rejected -> ContributeMessage.Rejected(message, contactData)
        is ContributionResult.RateLimited -> ContributeMessage.RateLimited(message, retryAfterSeconds?.let(PlaceFormat::waitTime))
        is ContributionResult.Failed -> ContributeMessage.Failed(failure)
        is ContributionResult.Published -> error("Publicada no es un mensaje")
    }
}

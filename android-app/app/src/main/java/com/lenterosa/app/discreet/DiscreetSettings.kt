package com.lenterosa.app.discreet

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Contacto de confianza: solo nombre y teléfono, guardados solo en este teléfono (RF-08, ADR 0013). */
@Serializable
data class TrustedContact(val name: String, val phone: String)

/** Marca de "me sentí incómoda aquí", guardada solo en este teléfono. La ubicación es aproximada y puede faltar. */
@Serializable
data class UncomfortableMark(val id: String, val atEpochMillis: Long, val lat: Double? = null, val lng: Double? = null)

/** Ajustes del Modo discreto. Nada de esto sale del teléfono ni necesita cuenta. */
data class DiscreetSettings(
    val contacts: List<TrustedContact> = emptyList(),
    val fakeCallName: String = DEFAULT_CALLER,
    val fakeCallDelaySeconds: Int = DEFAULT_DELAY_SECONDS,
) {
    companion object {
        const val DEFAULT_CALLER = "Mamá"
        const val DEFAULT_DELAY_SECONDS = 10
        val DELAY_OPTIONS = listOf(0, 10, 30)
        const val MAX_CONTACTS = 3
    }
}

interface DiscreetStore {
    val settings: Flow<DiscreetSettings>
    val marks: Flow<List<UncomfortableMark>>

    suspend fun addContact(contact: TrustedContact): Boolean
    suspend fun removeContact(contact: TrustedContact)
    suspend fun setFakeCall(name: String, delaySeconds: Int)
    suspend fun addMark(mark: UncomfortableMark)
    suspend fun removeMark(id: String)
}

@Singleton
class DataStoreDiscreetStore @Inject constructor(private val dataStore: DataStore<Preferences>) : DiscreetStore {

    private val json = Json { ignoreUnknownKeys = true }
    private val contactsSerializer = ListSerializer(TrustedContact.serializer())
    private val marksSerializer = ListSerializer(UncomfortableMark.serializer())

    override val settings: Flow<DiscreetSettings> = dataStore.data.map { prefs ->
        DiscreetSettings(
            contacts = prefs[CONTACTS]?.let { runCatching { json.decodeFromString(contactsSerializer, it) }.getOrNull() }.orEmpty(),
            fakeCallName = prefs[CALLER] ?: DiscreetSettings.DEFAULT_CALLER,
            fakeCallDelaySeconds = prefs[DELAY] ?: DiscreetSettings.DEFAULT_DELAY_SECONDS,
        )
    }

    override val marks: Flow<List<UncomfortableMark>> = dataStore.data.map { prefs ->
        prefs[MARKS]?.let { runCatching { json.decodeFromString(marksSerializer, it) }.getOrNull() }.orEmpty()
    }

    override suspend fun addContact(contact: TrustedContact): Boolean {
        val current = settings.first().contacts
        if (current.size >= DiscreetSettings.MAX_CONTACTS || contact.phone.isBlank()) return false
        dataStore.edit { it[CONTACTS] = json.encodeToString(contactsSerializer, current + contact) }
        return true
    }

    override suspend fun removeContact(contact: TrustedContact) {
        val current = settings.first().contacts
        dataStore.edit { it[CONTACTS] = json.encodeToString(contactsSerializer, current - contact) }
    }

    override suspend fun setFakeCall(name: String, delaySeconds: Int) {
        dataStore.edit {
            it[CALLER] = name.trim().ifEmpty { DiscreetSettings.DEFAULT_CALLER }
            it[DELAY] = delaySeconds.coerceIn(0, 60)
        }
    }

    // Se guardan las 50 marcas más recientes.
    override suspend fun addMark(mark: UncomfortableMark) {
        val current = marks.first()
        dataStore.edit { it[MARKS] = json.encodeToString(marksSerializer, (listOf(mark) + current).take(MAX_MARKS)) }
    }

    override suspend fun removeMark(id: String) {
        val current = marks.first()
        dataStore.edit { it[MARKS] = json.encodeToString(marksSerializer, current.filterNot { m -> m.id == id }) }
    }

    private companion object {
        const val MAX_MARKS = 50
        val CONTACTS = stringPreferencesKey("discreet_contacts")
        val CALLER = stringPreferencesKey("discreet_fake_call_name")
        val DELAY = intPreferencesKey("discreet_fake_call_delay")
        val MARKS = stringPreferencesKey("discreet_marks")
    }
}

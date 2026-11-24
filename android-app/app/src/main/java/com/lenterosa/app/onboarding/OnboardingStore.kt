package com.lenterosa.app.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Si la bienvenida ya se completó en este teléfono (RF-11). Solo se guarda esta marca, sin datos personales. */
interface OnboardingStore {
    val completed: Flow<Boolean>

    suspend fun markCompleted()
}

@Singleton
class DataStoreOnboardingStore @Inject constructor(private val dataStore: DataStore<Preferences>) : OnboardingStore {

    override val completed: Flow<Boolean> = dataStore.data.map { it[COMPLETED] ?: false }

    override suspend fun markCompleted() {
        dataStore.edit { it[COMPLETED] = true }
    }

    private companion object {
        val COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}

/** Decide la primera pantalla: nulo mientras se lee la marca guardada. */
@HiltViewModel
class LaunchViewModel @Inject constructor(store: OnboardingStore) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> = store.completed
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val store: OnboardingStore) : ViewModel() {

    /** Guarda que la bienvenida terminó (también al saltarla) y luego avisa a la navegación. */
    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            store.markCompleted()
            onFinished()
        }
    }
}

package com.lenterosa.app.discreet

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiscreetStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `los ajustes se guardan solo en el teléfono y admiten hasta 3 contactos`() = runTest {
        val store = DataStoreDiscreetStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "ajustes.preferences_pb") })

        assertEquals(DiscreetSettings(), store.settings.first())
        repeat(3) { assertTrue(store.addContact(TrustedContact("C$it", "30012345$it"))) }
        assertFalse(store.addContact(TrustedContact("Cuarta", "3009999999")))
        assertFalse(store.addContact(TrustedContact("Sin teléfono", " ")))
        store.removeContact(TrustedContact("C0", "300123450"))
        assertEquals(listOf("C1", "C2"), store.settings.first().contacts.map { it.name })

        store.setFakeCall("  ", 99)
        assertEquals(DiscreetSettings.DEFAULT_CALLER, store.settings.first().fakeCallName)
        assertEquals(60, store.settings.first().fakeCallDelaySeconds)

        store.addMark(UncomfortableMark("m1", 10L, 1.2, -77.3))
        store.removeMark("m1")
        assertTrue(store.marks.first().isEmpty())
    }
}

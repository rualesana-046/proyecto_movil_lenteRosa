package com.lenterosa.app.privacy

import androidx.test.core.app.ApplicationProvider
import com.lenterosa.app.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * TD-03: los datos del Modo discreto (contactos de confianza, marcas y ajustes en files/datastore),
 * la sesión y los tokens (shared_prefs) y la caché del mapa (files) nunca salen del teléfono: ni en la
 * copia de seguridad en la nube ni en la transferencia a otro dispositivo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class BackupRulesTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    // Todos los dominios de datos de la app que admite cada archivo de reglas.
    private val extractionDomains = setOf(
        "root", "file", "database", "sharedpref", "external",
        "device_root", "device_file", "device_database", "device_sharedpref",
    )
    private val fullBackupDomains = setOf("root", "file", "database", "sharedpref", "external")

    @Test
    fun `la nube y la transferencia entre dispositivos excluyen todos los dominios, sin ninguna inclusión`() {
        val rules = parse(R.xml.data_extraction_rules)

        listOf("cloud-backup", "device-transfer").forEach { section ->
            val excluded = rules.filter { it.section == section && it.tag == "exclude" }
            assertEquals("Dominios excluidos en $section", extractionDomains, excluded.map { it.domain }.toSet())
            assertTrue("Cada exclusión cubre todo el dominio en $section", excluded.all { it.path == "." })
            assertTrue("No hay inclusiones en $section", rules.none { it.section == section && it.tag == "include" })
        }
    }

    @Test
    fun `en Android 11 o inferior tampoco hay copia de seguridad`() {
        val rules = parse(R.xml.backup_rules)

        assertEquals(fullBackupDomains, rules.filter { it.tag == "exclude" }.map { it.domain }.toSet())
        assertTrue(rules.filter { it.tag == "exclude" }.all { it.path == "." })
        assertTrue(rules.none { it.tag == "include" })
    }

    @Test
    fun `donde la app guarda datos sensibles queda dentro de los dominios excluidos`() {
        // files/datastore: Modo discreto y ajustes; shared_prefs: sesión de Firebase; files: caché del mapa.
        val sensitive = mapOf("files/datastore/ajustes.preferences_pb" to "file", "shared_prefs" to "sharedpref",
            "files/mbgl-offline.db" to "file")
        val excluded = parse(R.xml.data_extraction_rules).filter { it.tag == "exclude" }.map { it.domain }.toSet()
        sensitive.forEach { (path, domain) -> assertTrue("$path ($domain) no está excluido", domain in excluded) }
    }

    @Test
    fun `el manifiesto declara las reglas y desactiva la copia de seguridad`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:allowBackup="false""""))
        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
        assertTrue(manifest.contains("""android:fullBackupContent="@xml/backup_rules""""))
    }

    private data class Rule(val section: String?, val tag: String, val domain: String?, val path: String?)

    private fun parse(resource: Int): List<Rule> {
        val parser = context.resources.getXml(resource)
        val rules = mutableListOf<Rule>()
        var section: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "cloud-backup", "device-transfer" -> section = parser.name
                "include", "exclude" -> rules += Rule(section, parser.name, parser.getAttributeValue(null, "domain"),
                    parser.getAttributeValue(null, "path"))
            }
        }
        return rules
    }
}

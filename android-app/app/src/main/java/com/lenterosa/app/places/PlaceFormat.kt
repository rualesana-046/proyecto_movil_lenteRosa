package com.lenterosa.app.places

import androidx.annotation.StringRes
import com.lenterosa.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

/** Formatos en español de Colombia (coma decimal) para las pantallas de lugares. */
object PlaceFormat {

    private val spanish: Locale = Locale.forLanguageTag("es-CO")

    @StringRes
    fun category(code: String): Int = when (code) {
        "PARK" -> R.string.category_park
        "STREET" -> R.string.category_street
        "TRANSPORT" -> R.string.category_transport
        "COMMERCE" -> R.string.category_commerce
        "EDUCATION" -> R.string.category_education
        "HEALTH" -> R.string.category_health
        "NIGHTLIFE" -> R.string.category_nightlife
        else -> R.string.category_other
    }

    /** "350 m" o "1,2 km". */
    fun distance(meters: Long): String =
        if (meters < 1_000) "$meters m" else String.format(spanish, "%.1f km", meters / 1_000.0)

    /** "4,2" con un decimal, o nulo cuando no hay datos (la pantalla muestra "Sin datos"). */
    fun average(value: Double?): String? = value?.let { String.format(spanish, "%.1f", it) }

    /** "12 nov 2026" en la zona horaria del teléfono; nulo si la fecha no se puede leer. */
    fun date(isoInstant: String?, zone: ZoneId = ZoneId.systemDefault()): String? = try {
        isoInstant?.let { DateTimeFormatter.ofPattern("d MMM yyyy", spanish).withZone(zone).format(Instant.parse(it)) }
    } catch (_: Exception) {
        null
    }

    /** Tiempo de espera redondeado hacia arriba a días, horas o minutos. */
    fun waitTime(seconds: Long): WaitTime = when {
        seconds >= DAY -> WaitTime(ceil(seconds / DAY.toDouble()).toInt(), WaitUnit.DAYS)
        seconds >= HOUR -> WaitTime(ceil(seconds / HOUR.toDouble()).toInt(), WaitUnit.HOURS)
        else -> WaitTime(maxOf(1, ceil(seconds / 60.0).toInt()), WaitUnit.MINUTES)
    }

    private const val HOUR = 3_600L
    private const val DAY = 24 * HOUR
}

enum class WaitUnit { DAYS, HOURS, MINUTES }

data class WaitTime(val amount: Int, val unit: WaitUnit)

package com.lenterosa.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.lenterosa.app.R

// Plus Jakarta Sans como fuente descargable de Google Fonts (ADR 0008): el sistema la descarga una vez
// y la comparte entre apps. Si no está disponible (sin Google Play o sin conexión la primera vez), se usa
// la fuente del sistema y la app sigue funcionando.
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val plusJakartaSans = GoogleFont("Plus Jakarta Sans")

internal val PlusJakartaSans = FontFamily(
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.jakarta(weight: FontWeight? = null) =
    copy(fontFamily = PlusJakartaSans, fontWeight = weight ?: fontWeight)

internal val LenteRosaTypography = Typography(
    displayLarge = base.displayLarge.jakarta(FontWeight.Bold),
    displayMedium = base.displayMedium.jakarta(FontWeight.Bold),
    displaySmall = base.displaySmall.jakarta(FontWeight.Bold),
    headlineLarge = base.headlineLarge.jakarta(FontWeight.Bold),
    headlineMedium = base.headlineMedium.jakarta(FontWeight.Bold),
    headlineSmall = base.headlineSmall.jakarta(FontWeight.SemiBold),
    titleLarge = base.titleLarge.jakarta(FontWeight.SemiBold),
    titleMedium = base.titleMedium.jakarta(FontWeight.SemiBold),
    titleSmall = base.titleSmall.jakarta(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.jakarta(),
    bodyMedium = base.bodyMedium.jakarta(),
    bodySmall = base.bodySmall.jakarta(),
    labelLarge = base.labelLarge.jakarta(FontWeight.SemiBold),
    labelMedium = base.labelMedium.jakarta(FontWeight.Medium),
    labelSmall = base.labelSmall.jakarta(FontWeight.Medium),
)

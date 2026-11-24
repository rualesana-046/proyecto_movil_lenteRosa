package com.lenterosa.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.lenterosa.app.R

// Plus Jakarta Sans incluida en la app (licencia SIL OFL 1.1, en assets/licenses): funciona sin conexión
// y sin Google Play. Los archivos son los TTF estáticos del repositorio oficial tokotype/PlusJakartaSans (ADR 0008).
internal val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
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

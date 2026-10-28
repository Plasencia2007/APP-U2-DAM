package pe.upeu.ecorecicla.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import pe.upeu.ecorecicla.recursos.Res
import pe.upeu.ecorecicla.recursos.barlow_bold
import pe.upeu.ecorecicla.recursos.barlow_medium
import pe.upeu.ecorecicla.recursos.barlow_regular
import pe.upeu.ecorecicla.recursos.barlow_semibold
import pe.upeu.ecorecicla.recursos.barlow_semicondensed_bold
import pe.upeu.ecorecicla.recursos.barlow_semicondensed_semibold

/*
 * Sistema visual de EcoRutas.
 * Pensado para un operario en ruta: luz de dia, sotanos, guantes y miradas rapidas.
 * - Verde bosque como base de marca; ambar "chaleco de seguridad" para lo pendiente o sin senal;
 *   rojo oxido para anulaciones y errores.
 * - Barlow (inspirada en la senaletica vial) para todo el texto y Barlow Semi Condensed para
 *   los kilos y los encabezados: el peso es el dato protagonista.
 */

object Paleta {
    val Bosque = Color(0xFF0F3D2E)        // marca, encabezados, acciones principales
    val BosqueProfundo = Color(0xFF0A2A20)
    val Hoja = Color(0xFF2F9E6B)          // pesado / completado
    val HojaSuave = Color(0xFFDDF2E7)
    val Ambar = Color(0xFFE8A317)         // pendiente, sin senal
    val AmbarSuave = Color(0xFFFFF1CF)
    val Oxido = Color(0xFFB4441F)         // anulado, errores
    val OxidoSuave = Color(0xFFFBE4DA)
    val Niebla = Color(0xFFF2F5F3)        // fondo
    val Carbon = Color(0xFF17201C)        // texto principal
    val Pizarra = Color(0xFF5B6B63)       // texto secundario
    val Linea = Color(0xFFD5DED9)         // trazos del recorrido y bordes
}

/** Colores semanticos que Material 3 no cubre (estados de una parada o de un pesaje). */
@Immutable
data class ColoresEco(
    val completado: Color,
    val completadoFondo: Color,
    val pendiente: Color,
    val pendienteFondo: Color,
    val anulado: Color,
    val anuladoFondo: Color,
    val trazo: Color,
    val textoSecundario: Color
)

private val EcoClaro = ColoresEco(
    completado = Paleta.Hoja, completadoFondo = Paleta.HojaSuave,
    pendiente = Paleta.Ambar, pendienteFondo = Paleta.AmbarSuave,
    anulado = Paleta.Oxido, anuladoFondo = Paleta.OxidoSuave,
    trazo = Paleta.Linea, textoSecundario = Paleta.Pizarra
)

private val EcoOscuro = ColoresEco(
    completado = Color(0xFF5FCB97), completadoFondo = Color(0xFF173A2B),
    pendiente = Color(0xFFF2C14E), pendienteFondo = Color(0xFF3D3013),
    anulado = Color(0xFFF08A63), anuladoFondo = Color(0xFF3F2117),
    trazo = Color(0xFF34443C), textoSecundario = Color(0xFFA7B6AE)
)

val LocalColoresEco = staticCompositionLocalOf { EcoClaro }

private val EsquemaClaro = lightColorScheme(
    primary = Paleta.Bosque,
    onPrimary = Color.White,
    primaryContainer = Paleta.HojaSuave,
    onPrimaryContainer = Paleta.BosqueProfundo,
    secondary = Paleta.Hoja,
    onSecondary = Color.White,
    tertiary = Paleta.Ambar,
    onTertiary = Paleta.Carbon,
    tertiaryContainer = Paleta.AmbarSuave,
    onTertiaryContainer = Color(0xFF4A3300),
    error = Paleta.Oxido,
    errorContainer = Paleta.OxidoSuave,
    onErrorContainer = Color(0xFF4D1605),
    background = Paleta.Niebla,
    onBackground = Paleta.Carbon,
    surface = Color.White,
    onSurface = Paleta.Carbon,
    surfaceVariant = Color(0xFFE7EDE9),
    onSurfaceVariant = Paleta.Pizarra,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9F8),
    surfaceContainer = Paleta.Niebla,
    outline = Color(0xFFB9C6BF),
    outlineVariant = Paleta.Linea
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF7FD3A8),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1C4A38),
    onPrimaryContainer = Color(0xFFCBEFDC),
    secondary = Color(0xFF5FCB97),
    tertiary = Color(0xFFF2C14E),
    tertiaryContainer = Color(0xFF3D3013),
    onTertiaryContainer = Color(0xFFFFE3A3),
    error = Color(0xFFF08A63),
    errorContainer = Color(0xFF3F2117),
    onErrorContainer = Color(0xFFFFDBCF),
    background = Color(0xFF0D1512),
    onBackground = Color(0xFFE2EAE5),
    surface = Color(0xFF141E1A),
    onSurface = Color(0xFFE2EAE5),
    surfaceVariant = Color(0xFF22302A),
    onSurfaceVariant = Color(0xFFA7B6AE),
    outline = Color(0xFF52625A),
    outlineVariant = Color(0xFF2C3A33)
)

val FormasEco = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
private fun tipografia(): Typography {
    val barlow = FontFamily(
        Font(Res.font.barlow_regular, FontWeight.Normal),
        Font(Res.font.barlow_medium, FontWeight.Medium),
        Font(Res.font.barlow_semibold, FontWeight.SemiBold),
        Font(Res.font.barlow_bold, FontWeight.Bold)
    )
    val condensada = FontFamily(
        Font(Res.font.barlow_semicondensed_semibold, FontWeight.SemiBold),
        Font(Res.font.barlow_semicondensed_bold, FontWeight.Bold)
    )
    fun s(f: FontFamily, w: FontWeight, tam: Int, alto: Int, esp: Double = 0.0) =
        TextStyle(fontFamily = f, fontWeight = w, fontSize = tam.sp, lineHeight = alto.sp, letterSpacing = esp.sp)

    return Typography(
        displayLarge = s(condensada, FontWeight.Bold, 56, 58, -0.5),
        displayMedium = s(condensada, FontWeight.Bold, 44, 48, -0.25),
        displaySmall = s(condensada, FontWeight.Bold, 36, 40),
        headlineLarge = s(condensada, FontWeight.Bold, 32, 36),
        headlineMedium = s(condensada, FontWeight.Bold, 28, 32),
        headlineSmall = s(condensada, FontWeight.SemiBold, 24, 28),
        titleLarge = s(barlow, FontWeight.SemiBold, 21, 26),
        titleMedium = s(barlow, FontWeight.SemiBold, 17, 22, 0.1),
        titleSmall = s(barlow, FontWeight.SemiBold, 15, 20, 0.1),
        bodyLarge = s(barlow, FontWeight.Normal, 17, 24),
        bodyMedium = s(barlow, FontWeight.Normal, 15, 21),
        bodySmall = s(barlow, FontWeight.Normal, 13, 18, 0.1),
        labelLarge = s(barlow, FontWeight.SemiBold, 16, 20, 0.2),
        labelMedium = s(barlow, FontWeight.Medium, 13, 16, 0.3),
        labelSmall = s(barlow, FontWeight.Medium, 12, 15, 0.3)
    )
}

@Composable
fun TemaEcoRutas(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalColoresEco provides if (oscuro) EcoOscuro else EcoClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = tipografia(),
            shapes = FormasEco,
            content = content
        )
    }
}

/** Acceso corto a los colores semanticos: MaterialTheme.eco.pendiente */
val MaterialTheme.eco: ColoresEco
    @Composable get() = LocalColoresEco.current

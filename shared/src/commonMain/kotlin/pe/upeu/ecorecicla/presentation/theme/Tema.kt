package pe.upeu.ecorecicla.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VerdeEco = Color(0xFF2E7D32)
val VerdeClaro = Color(0xFFC8E6C9)
val Ambar = Color(0xFFF9A825)
val AmbarClaro = Color(0xFFFFF3C4)
val RojoAnulado = Color(0xFFC62828)

private val Claro = lightColorScheme(
    primary = VerdeEco,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = Color(0xFF0B2E0F),
    secondary = Color(0xFF00695C),
    tertiary = Ambar,
    tertiaryContainer = AmbarClaro,
    error = RojoAnulado
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0B2E0F),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFD54F),
    tertiaryContainer = Color(0xFF5D4600),
    error = Color(0xFFEF9A9A)
)

@Composable
fun TemaEcoRutas(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = content)
}

package pe.upeu.ecorecicla.presentation.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.PathParser
import pe.upeu.ecorecicla.presentation.theme.Paleta

/**
 * Marca de EcoRutas: ruta en S desde un punto de partida hasta una parada con pin.
 * Usa los mismos trazos que el icono adaptativo de la app (ic_launcher_foreground.xml).
 */
@Composable
fun MarcaEcoRutas(modifier: Modifier = Modifier) {
    val ruta = remember { PathParser().parsePathString("M39,71 C39,58 66,66 66,54").toPath() }
    val pin = remember {
        PathParser().parsePathString("M66,56 C66,56 55,46 55,39.5 A11,11 0,1 1,77 39.5 C77,46 66,56 66,56 Z").toPath()
    }
    Canvas(modifier) {
        // El dibujo original ocupa la zona 28..82 de un lienzo de 108: se recorta y escala a este tamanio.
        val escala = size.minDimension / 54f
        scale(escala, pivot = Offset.Zero) {
            translate(left = -28f, top = -22f) {
                drawPath(ruta, androidx.compose.ui.graphics.Color(0xFF7FD3A8), style = Stroke(width = 5f, cap = StrokeCap.Round))
                drawCircle(androidx.compose.ui.graphics.Color.White, radius = 5.5f, center = Offset(39f, 71f))
                drawPath(pin, Paleta.Ambar)
                drawCircle(Paleta.Bosque, radius = 4.5f, center = Offset(66f, 39.5f))
            }
        }
    }
}


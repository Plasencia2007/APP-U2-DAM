package pe.upeu.ecorecicla.presentation.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.presentation.MensajeError
import pe.upeu.ecorecicla.presentation.fechaCorta
import pe.upeu.ecorecicla.presentation.numeroKg
import pe.upeu.ecorecicla.presentation.theme.eco

/** Icono de cada material reciclable; el resto usa el simbolo de reciclaje. */
fun iconoMaterial(nombre: String): ImageVector = when (nombre.trim().lowercase()) {
    "carton", "cartón" -> Icons.Filled.Inventory2
    "plastico", "plástico" -> Icons.Filled.LocalDrink
    "papel" -> Icons.Filled.Description
    "vidrio" -> Icons.Filled.WineBar
    "metal" -> Icons.Filled.Hardware
    else -> Icons.Filled.Recycling
}

/** Cifra en kilos con la unidad mas pequena: "45,5 kg". El peso es el dato protagonista. */
@Composable
fun CifraKg(
    valor: Double,
    estilo: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(numeroKg(valor), style = estilo, color = color)
        Spacer(Modifier.width(3.dp))
        Text(
            "kg",
            style = MaterialTheme.typography.labelLarge,
            color = color.copy(alpha = 0.75f),
            modifier = Modifier.padding(bottom = (estilo.fontSize.value * 0.12f).dp)
        )
    }
}

/** Barra de avance de la ruta: un segmento por parada, lleno cuando ya se peso. */
@Composable
fun ProgresoSegmentado(avance: List<Boolean>, colorLleno: Color, colorVacio: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        avance.forEach { lleno ->
            val color = if (lleno) colorLleno else colorVacio
            Box(Modifier.weight(1f).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

/** Etiqueta compacta de estado o atributo (zona, material, pendiente, anulado). */
@Composable
fun Etiqueta(
    texto: String,
    color: Color,
    fondo: Color,
    icono: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(fondo).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Estado de la senal, pensado para leerse de un vistazo sobre el encabezado verde. */
@Composable
fun EstadoSenal(conectado: Boolean, ultimaSincronizacion: String?, hoy: String? = null, modifier: Modifier = Modifier) {
    val eco = MaterialTheme.eco
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (conectado) Color.White.copy(alpha = 0.12f) else eco.pendiente)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (conectado) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
            contentDescription = null,
            tint = if (conectado) Color.White else Color(0xFF2B1D00),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                !conectado -> "Sin señal: trabajando con datos del equipo"
                ultimaSincronizacion == null -> "En línea, aún sin sincronizar"
                else -> "En línea, sincronizado ${fechaCorta(ultimaSincronizacion, hoy)}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (conectado) Color.White else Color(0xFF2B1D00)
        )
    }
}

/** Boton principal de ancho completo y 56 dp de alto: comodo de pulsar con guantes. */
@Composable
fun BotonPrincipal(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    contenedor: Color = MaterialTheme.colorScheme.primary,
    contenido: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = alPulsar,
        enabled = habilitado && !cargando,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = contenedor, contentColor = contenido),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = contenido)
        } else {
            if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Aviso en linea: confirmacion (verde) o error (oxido). "Reintentar" solo si el error lo admite. */
@Composable
fun BannerMensajes(
    mensaje: String?,
    error: MensajeError?,
    alReintentar: () -> Unit,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val texto = error?.texto ?: mensaje ?: return
    val esError = error != null
    val eco = MaterialTheme.eco
    val color = if (esError) eco.anulado else eco.completado
    Surface(
        color = if (esError) eco.anuladoFondo else eco.completadoFondo,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (esError) Icons.Filled.ErrorOutline else Icons.Filled.TaskAlt, contentDescription = null, tint = color)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (error?.reintentable == true) {
                    TextButton(onClick = alReintentar, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("Reintentar", color = color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            IconButton(onClick = alCerrar) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar aviso", tint = MaterialTheme.eco.textoSecundario)
            }
        }
    }
}

/** Nodo del recorrido: numero de parada o marca de completado. */
@Composable
fun NodoParada(secuencia: Int, completado: Boolean, modifier: Modifier = Modifier) {
    val eco = MaterialTheme.eco
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (completado) eco.completado else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (completado) eco.completado else MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (completado) {
            Icon(Icons.Filled.TaskAlt, contentDescription = "Pesado", tint = Color.White, modifier = Modifier.size(20.dp))
        } else {
            Text("$secuencia", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

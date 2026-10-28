package pe.upeu.ecorecicla.presentation.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.upeu.ecorecicla.presentation.MensajeError

/** Muestra un aviso informativo o un error; ofrece "Reintentar" solo si el error lo admite (500, tiempo agotado). */
@Composable
fun BannerMensajes(
    mensaje: String?,
    error: MensajeError?,
    alReintentar: () -> Unit,
    alCerrar: () -> Unit
) {
    val texto = error?.texto ?: mensaje ?: return
    val esError = error != null
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(texto, style = MaterialTheme.typography.bodyMedium)
                if (error?.reintentable == true) {
                    TextButton(onClick = alReintentar) { Text("Reintentar") }
                }
            }
            IconButton(onClick = alCerrar) { Icon(Icons.Default.Close, contentDescription = "Cerrar aviso") }
        }
    }
}

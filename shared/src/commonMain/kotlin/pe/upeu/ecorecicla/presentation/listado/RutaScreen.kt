package pe.upeu.ecorecicla.presentation.listado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.domain.model.EstadoRuta
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.presentation.componentes.BannerMensajes
import pe.upeu.ecorecicla.presentation.fechaCorta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutaScreen(
    alAbrirPunto: (Long) -> Unit,
    alAbrirPerfil: () -> Unit,
    vm: RutaViewModel = koinViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarCierre by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ruta del dia", fontWeight = FontWeight.SemiBold)
                        estado.ruta?.let {
                            Text("${it.fecha} · ${estado.pesados}/${it.puntos.size} pesados",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = vm::sincronizar, enabled = !estado.sincronizando) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                    }
                    IconButton(onClick = alAbrirPerfil) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            if (estado.sincronizando) LinearProgressIndicator(Modifier.fillMaxWidth())

            EstadoConexion(estado.conectado, estado.ultimaSincronizacion)

            BannerMensajes(
                mensaje = estado.mensaje,
                error = estado.error,
                alReintentar = vm::sincronizar,
                alCerrar = vm::descartarMensajes
            )

            OutlinedTextField(
                value = estado.busqueda,
                onValueChange = vm::buscar,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Buscar por razon social o zona") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val ruta = estado.ruta
            if (ruta != null && ruta.estado == EstadoRuta.CERRADA) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Ruta cerrada: ya no admite pesajes", color = MaterialTheme.colorScheme.error)
                }
            } else if (ruta != null && estado.puedeCerrarRuta) {
                // Solo se ofrece a quien tiene RUTA_CERRAR; el servicio vuelve a verificarlo.
                TextButton(onClick = { confirmarCierre = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cerrar ruta del dia")
                }
            }

            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                estado.vacia -> ListaVacia(estado.busqueda.isNotBlank(), estado.conectado)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ruta!!.puntos, key = { it.id }) { punto ->
                        TarjetaPunto(punto) { alAbrirPunto(punto.id) }
                    }
                }
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("Cerrar la ruta") },
            text = { Text("Una ruta cerrada ya no admite pesajes ni correcciones. ¿Desea cerrarla?") },
            confirmButton = {
                TextButton(onClick = { confirmarCierre = false; vm.cerrarRuta() }) { Text("Cerrar ruta") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EstadoConexion(conectado: Boolean, ultima: String?) {
    val color = if (conectado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            (if (conectado) "En linea" else "Sin conexion") + " · Ultima sincronizacion: ${fechaCorta(ultima)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TarjetaPunto(punto: PuntoAcopio, alPulsar: () -> Unit) {
    val pesado = punto.atendido
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
        colors = CardDefaults.cardColors(
            containerColor = if (pesado) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("${punto.secuencia}", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(punto.razonSocial, fontWeight = FontWeight.SemiBold)
                Text("${punto.direccion} · Zona ${punto.zona}", style = MaterialTheme.typography.bodySmall)
                Text("Material habitual: ${punto.materialHabitual}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            if (pesado) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Pesado", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(if (punto.omitido) "Omitido" else "Por pesar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ListaVacia(porBusqueda: Boolean, conectado: Boolean) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            when {
                porBusqueda -> "Ningun punto coincide con la busqueda."
                conectado -> "No hay una ruta asignada para hoy. Pulse sincronizar para consultarla."
                else -> "Aun no hay datos en el equipo. Sincronice cuando tenga senal."
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

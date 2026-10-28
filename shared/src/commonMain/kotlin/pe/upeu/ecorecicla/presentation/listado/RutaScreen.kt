package pe.upeu.ecorecicla.presentation.listado

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.domain.model.EstadoRuta
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.presentation.componentes.BannerMensajes
import pe.upeu.ecorecicla.presentation.componentes.CifraKg
import pe.upeu.ecorecicla.presentation.componentes.EstadoSenal
import pe.upeu.ecorecicla.presentation.componentes.Etiqueta
import pe.upeu.ecorecicla.presentation.componentes.iconoMaterial
import pe.upeu.ecorecicla.presentation.componentes.NodoParada
import pe.upeu.ecorecicla.presentation.componentes.ProgresoSegmentado
import pe.upeu.ecorecicla.presentation.fechaLarga
import pe.upeu.ecorecicla.presentation.theme.Paleta
import pe.upeu.ecorecicla.presentation.theme.eco

@Composable
fun RutaScreen(
    alAbrirPunto: (Long) -> Unit,
    alAbrirPerfil: () -> Unit,
    vm: RutaViewModel = koinViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarCierre by remember { mutableStateOf(false) }
    val ruta = estado.ruta

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Encabezado(estado, alSincronizar = vm::sincronizar, alAbrirPerfil = alAbrirPerfil)

        when {
            estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ruta == null && estado.busqueda.isBlank() -> SinRuta(estado.conectado, estado.sincronizando, vm::sincronizar)
            else -> Column(Modifier.fillMaxSize()) {
                // Buscador y avisos fijos: no se desplazan con la lista.
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Buscador(estado.busqueda, vm::buscar)
                    BannerMensajes(estado.mensaje, estado.error, alReintentar = vm::sincronizar, alCerrar = vm::descartarMensajes)
                }
                LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                modifier = Modifier.fillMaxSize().navigationBarsPadding()
            ) {
                if (ruta?.estado == EstadoRuta.CERRADA) {
                    item { AvisoRutaCerrada() }
                }
                item {
                    Text(
                        if (estado.busqueda.isBlank()) "Paradas en orden de visita" else "Resultados de la búsqueda",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.eco.textoSecundario,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }
                val puntos = ruta?.puntos.orEmpty()
                if (puntos.isEmpty()) {
                    item {
                        Text("Ninguna parada coincide con \"${estado.busqueda}\".", style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp))
                    }
                }
                itemsIndexed(puntos, key = { _, p -> p.id }) { i, punto ->
                    ParadaDelRecorrido(
                        punto = punto,
                        esPrimera = i == 0,
                        esUltima = i == puntos.lastIndex,
                        alPulsar = { alAbrirPunto(punto.id) }
                    )
                }
                // Solo se ofrece a quien tiene RUTA_CERRAR; el servicio vuelve a verificarlo (403 / 409 RN-04).
                if (ruta != null && ruta.estado == EstadoRuta.ABIERTA && estado.puedeCerrarRuta && estado.busqueda.isBlank()) {
                    item {
                        OutlinedButton(
                            onClick = { confirmarCierre = true },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Cerrar la ruta del día", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            if (estado.completa) "Todas las paradas tienen pesaje."
                            else (estado.totalParadas - estado.pesados).let { n ->
                                if (n == 1) "Falta 1 parada por pesar u omitir." else "Faltan $n paradas por pesar u omitir."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.eco.textoSecundario,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                        )
                    }
                }
            }
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text("Cerrar la ruta del día") },
            text = { Text("Después de cerrarla no se podrán registrar ni corregir pesajes de esta ruta.") },
            confirmButton = { TextButton(onClick = { confirmarCierre = false; vm.cerrarRuta() }) { Text("Cerrar ruta") } },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Encabezado(estado: RutaUiState, alSincronizar: () -> Unit, alAbrirPerfil: () -> Unit) {
    val ruta = estado.ruta
    Column(
        Modifier
            .fillMaxWidth()
            .background(Paleta.Bosque)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (ruta != null) "Ruta del ${fechaLarga(ruta.fecha)}" else "Ruta del día",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                if (estado.nombreUsuario.isNotBlank()) {
                    Text(estado.nombreUsuario, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                }
            }
            BotonSincronizar(estado.sincronizando, alSincronizar)
            Avatar(estado.nombreUsuario, alAbrirPerfil)
        }

        if (ruta != null) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(end = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${estado.pesados}", style = MaterialTheme.typography.displayMedium, color = Color.White)
                        Text(" de ${estado.totalParadas}", style = MaterialTheme.typography.headlineSmall,
                            color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Text("paradas pesadas", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    CifraKg(ruta.netoTotal, MaterialTheme.typography.displaySmall, color = Color.White)
                    Text("netos recolectados", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.height(14.dp))
            ProgresoSegmentado(
                avance = ruta.avance,
                colorLleno = Color(0xFF7FD3A8),
                colorVacio = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        EstadoSenal(estado.conectado, estado.ultimaSincronizacion, hoy = ruta?.fecha)
    }
}

@Composable
private fun BotonSincronizar(sincronizando: Boolean, alPulsar: () -> Unit) {
    val giro = rememberInfiniteTransition()
    val angulo by giro.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart)
    )
    IconButton(onClick = alPulsar, enabled = !sincronizando) {
        Icon(
            Icons.Filled.Sync,
            contentDescription = "Sincronizar ahora",
            tint = Color.White,
            modifier = if (sincronizando) Modifier.rotate(angulo) else Modifier
        )
    }
}

@Composable
private fun Avatar(nombre: String, alPulsar: () -> Unit) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.padding(start = 4.dp).size(40.dp).clip(CircleShape).background(Paleta.Ambar).clickable(onClick = alPulsar),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales.ifBlank { "?" }, style = MaterialTheme.typography.titleSmall, color = Paleta.Carbon)
    }
}

@Composable
private fun Buscador(texto: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = texto,
        onValueChange = alCambiar,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text("Buscar empresa o zona") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Una parada del recorrido: nodo numerado unido por una linea a la anterior y a la siguiente.
 * La linea se pinta verde hasta donde la ruta ya avanzo.
 */
@Composable
private fun ParadaDelRecorrido(punto: PuntoAcopio, esPrimera: Boolean, esUltima: Boolean, alPulsar: () -> Unit) {
    val eco = MaterialTheme.eco
    val colorLinea = if (punto.atendido) eco.completado else eco.trazo
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(44.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(3.dp).height(18.dp).background(if (esPrimera) Color.Transparent else colorLinea))
            NodoParada(punto.secuencia, punto.atendido)
            Box(Modifier.width(3.dp).weight(1f).background(if (esUltima) Color.Transparent else colorLinea))
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = alPulsar,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .border(1.dp, if (punto.atendido) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
        ) {
            Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(punto.razonSocial, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(punto.direccion, style = MaterialTheme.typography.bodyMedium, color = eco.textoSecundario,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        // Solo lo que pide atencion lleva color: las paradas por pesar.
                        when {
                            punto.atendido -> {}
                            punto.omitido -> Etiqueta("Omitida", eco.textoSecundario, MaterialTheme.colorScheme.surfaceVariant)
                            else -> Etiqueta("Por pesar", Color(0xFF6B4A00), eco.pendienteFondo)
                        }
                        Etiqueta("Zona ${punto.zona}", eco.textoSecundario, MaterialTheme.colorScheme.surfaceVariant, Icons.Filled.Place)
                        Etiqueta(punto.materialHabitual, eco.textoSecundario, MaterialTheme.colorScheme.surfaceVariant, iconoMaterial(punto.materialHabitual))
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = eco.textoSecundario)
            }
        }
    }
}

@Composable
private fun AvisoRutaCerrada() {
    val eco = MaterialTheme.eco
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(MaterialTheme.shapes.medium).background(eco.anuladoFondo).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = eco.anulado)
        Spacer(Modifier.width(10.dp))
        Text("La ruta está cerrada. Ya no admite pesajes ni correcciones.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SinRuta(conectado: Boolean, sincronizando: Boolean, alSincronizar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.eco.pendiente, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            if (conectado) "No tienes una ruta asignada para hoy" else "Aún no hay datos en este equipo",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (conectado) "Sincroniza para consultar si ya te asignaron una."
            else "Conéctate a una red y sincroniza para descargar la ruta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.eco.textoSecundario
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = alSincronizar, enabled = conectado && !sincronizando, shape = MaterialTheme.shapes.medium) {
            Icon(Icons.Filled.Sync, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sincronizar ahora")
        }
    }
}

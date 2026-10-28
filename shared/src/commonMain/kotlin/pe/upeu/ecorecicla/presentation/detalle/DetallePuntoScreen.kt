package pe.upeu.ecorecicla.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.presentation.componentes.BannerMensajes
import pe.upeu.ecorecicla.presentation.fechaCorta
import pe.upeu.ecorecicla.presentation.kg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePuntoScreen(
    puntoId: Long,
    alVolver: () -> Unit,
    alRegistrar: (rutaId: Long) -> Unit,
    alCorregir: (rutaId: Long, pesajeId: Long) -> Unit,
    vm: DetallePuntoViewModel = koinViewModel()
) {
    LaunchedEffect(puntoId) { vm.seleccionar(puntoId) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var anulando by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.punto?.razonSocial ?: "Punto de acopio") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        },
        floatingActionButton = {
            // "Registrar pesaje" solo para quien tiene PESAJE_CREAR y con la ruta abierta.
            if (estado.puedeRegistrar) {
                ExtendedFloatingActionButton(
                    onClick = { alRegistrar(estado.rutaId) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Registrar pesaje") }
                )
            }
        }
    ) { relleno ->
        Column(Modifier.padding(relleno).fillMaxSize()) {
            BannerMensajes(estado.mensaje, estado.error, alReintentar = {}, alCerrar = vm::descartarMensajes)
            val punto = estado.punto
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                punto == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("El punto ya no forma parte de la ruta del dia.")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Visita N.° ${punto.secuencia}", style = MaterialTheme.typography.labelLarge)
                                Text(punto.razonSocial, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(punto.direccion)
                                Text("Zona ${punto.zona} · Material habitual: ${punto.materialHabitual}")
                                Text(if (punto.atendido) "Estado: pesado" else "Estado: por pesar",
                                    color = if (punto.atendido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                                if (estado.rutaCerrada) Text("La ruta esta cerrada.", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    item { Text("Pesajes (${estado.pesajes.size})", style = MaterialTheme.typography.titleMedium) }
                    if (estado.pesajes.isEmpty()) {
                        item { Text("Este punto aun no tiene pesajes.", style = MaterialTheme.typography.bodyMedium) }
                    }
                    items(estado.pesajes, key = { it.pesaje.id }) { ui ->
                        TarjetaPesaje(
                            ui = ui,
                            alCorregir = { alCorregir(estado.rutaId, ui.pesaje.id) },
                            alAnular = { anulando = ui.pesaje.id },
                            alDescartar = { vm.descartar(ui.pesaje.id) }
                        )
                    }
                }
            }
        }
    }

    anulando?.let { id ->
        DialogoAnular(
            alConfirmar = { motivo -> anulando = null; vm.anular(id, motivo) },
            alCancelar = { anulando = null }
        )
    }
}

@Composable
private fun TarjetaPesaje(ui: PesajeUi, alCorregir: () -> Unit, alAnular: () -> Unit, alDescartar: () -> Unit) {
    val p = ui.pesaje
    val anulado = p.estado as? EstadoPesaje.Anulado
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                p.pendiente -> MaterialTheme.colorScheme.tertiaryContainer
                anulado != null -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.material, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    textDecoration = if (anulado != null) TextDecoration.LineThrough else null)
                Text("Neto ${kg(p.pesoNeto)}", fontWeight = FontWeight.Bold)
            }
            Text("Bruto ${kg(p.pesoBruto)} · Tara ${kg(p.tara)}", style = MaterialTheme.typography.bodySmall)
            Text("${fechaCorta(p.fechaHora)} · ${p.operario} · id ${p.id}", style = MaterialTheme.typography.bodySmall)
            p.observacion?.let { Text("Obs.: $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.pendiente) AssistChip(onClick = {}, label = { Text("Pendiente de envio") })
                if (anulado != null) AssistChip(onClick = {}, label = { Text("Anulado: ${anulado.motivo}") })
            }
            p.errorSincronizacion?.let {
                Text("El servidor lo rechazo: $it", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Row {
                if (ui.puedeCorregir) TextButton(onClick = alCorregir) { Text("Corregir") }
                // "Anular" solo aparece para quien tiene PESAJE_ANULAR.
                if (ui.puedeAnular) TextButton(onClick = alAnular) { Text("Anular", color = MaterialTheme.colorScheme.error) }
                if (ui.puedeDescartar) TextButton(onClick = alDescartar) { Text("Descartar") }
            }
        }
    }
}

@Composable
private fun DialogoAnular(alConfirmar: (String) -> Unit, alCancelar: () -> Unit) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Anular pesaje") },
        text = {
            Column {
                Text("El pesaje no se borra: queda marcado como anulado.")
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo") },
                    isError = motivo.isBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { alConfirmar(motivo) }, enabled = motivo.isNotBlank()) { Text("Anular") } },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

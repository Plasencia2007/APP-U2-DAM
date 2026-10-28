package pe.upeu.ecorecicla.presentation.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.presentation.componentes.BannerMensajes
import pe.upeu.ecorecicla.presentation.componentes.BotonPrincipal
import pe.upeu.ecorecicla.presentation.componentes.CifraKg
import pe.upeu.ecorecicla.presentation.componentes.Etiqueta
import pe.upeu.ecorecicla.presentation.componentes.iconoMaterial
import pe.upeu.ecorecicla.presentation.hora
import pe.upeu.ecorecicla.presentation.numeroKg
import pe.upeu.ecorecicla.presentation.theme.Paleta
import pe.upeu.ecorecicla.presentation.theme.eco

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
    val punto = estado.punto

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Encabezado de la parada
        Column(Modifier.fillMaxWidth().background(Paleta.Bosque).statusBarsPadding().padding(start = 4.dp, end = 20.dp, bottom = 20.dp)) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la ruta", tint = Color.White)
            }
            if (punto != null) {
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        if (estado.totalParadas > 0) "Parada ${punto.secuencia} de ${estado.totalParadas}" else "Parada ${punto.secuencia}",
                        style = MaterialTheme.typography.titleSmall, color = Color(0xFF7FD3A8)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(punto.razonSocial, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${punto.direccion}, zona ${punto.zona}", style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Etiqueta("Suele entregar ${punto.materialHabitual.lowercase()}", Color.White,
                            Color.White.copy(alpha = 0.14f), iconoMaterial(punto.materialHabitual))
                        if (punto.atendido) {
                            Etiqueta("Pesado", Paleta.BosqueProfundo, Color(0xFF7FD3A8))
                        } else {
                            Etiqueta("Por pesar", Paleta.Carbon, Paleta.Ambar)
                        }
                    }
                }
            }
        }

        when {
            estado.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            punto == null -> Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Esta parada ya no forma parte de la ruta del día.", style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BannerMensajes(estado.mensaje, estado.error, alReintentar = {}, alCerrar = vm::descartarMensajes)
                }
                if (estado.rutaCerrada) {
                    item {
                        Etiqueta("Ruta cerrada: solo lectura", MaterialTheme.eco.anulado, MaterialTheme.eco.anuladoFondo, Icons.Filled.Lock)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
                        Text("Pesajes de esta parada", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        if (estado.pesajes.isNotEmpty()) {
                            Text("${numeroKg(estado.netoPunto)} kg netos", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.eco.textoSecundario)
                        }
                    }
                }
                if (estado.pesajes.isEmpty()) {
                    item { SinPesajes(estado.puedeRegistrar) }
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

        // "Registrar pesaje" solo para quien tiene PESAJE_CREAR y con la ruta abierta.
        if (estado.puedeRegistrar && punto != null) {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                BotonPrincipal(
                    texto = "Registrar pesaje",
                    icono = Icons.Filled.Add,
                    alPulsar = { alRegistrar(estado.rutaId) },
                    modifier = Modifier.navigationBarsPadding().padding(16.dp)
                )
            }
        }
    }

    anulando?.let { id ->
        DialogoAnular(alConfirmar = { motivo -> anulando = null; vm.anular(id, motivo) }, alCancelar = { anulando = null })
    }
}

@Composable
private fun TarjetaPesaje(ui: PesajeUi, alCorregir: () -> Unit, alAnular: () -> Unit, alDescartar: () -> Unit) {
    val p = ui.pesaje
    val eco = MaterialTheme.eco
    val anulado = p.estado as? EstadoPesaje.Anulado
    val colorCifra = when {
        anulado != null -> eco.textoSecundario
        p.pendiente -> Color(0xFF6B4A00)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = when {
            p.pendiente -> eco.pendienteFondo
            anulado != null -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconoMaterial(p.material), contentDescription = null, tint = eco.textoSecundario, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            p.material,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (anulado != null) TextDecoration.LineThrough else null
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Bruto ${numeroKg(p.pesoBruto)} kg, tara ${numeroKg(p.tara)} kg",
                        style = MaterialTheme.typography.bodyMedium, color = eco.textoSecundario)
                    Text("${hora(p.fechaHora)} por ${p.operario}", style = MaterialTheme.typography.bodySmall, color = eco.textoSecundario)
                    p.observacion?.let {
                        Text("“$it”", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    CifraKg(
                        p.pesoNeto,
                        MaterialTheme.typography.headlineLarge.copy(
                            textDecoration = if (anulado != null) TextDecoration.LineThrough else null
                        ),
                        color = colorCifra
                    )
                    Text("neto", style = MaterialTheme.typography.labelSmall, color = eco.textoSecundario)
                }
            }

            if (p.pendiente || anulado != null || p.errorSincronizacion != null) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.pendiente) {
                        Etiqueta(if (p.id < 0) "Sin enviar, id provisional ${p.id}" else "Cambio sin enviar",
                            Paleta.Carbon, Paleta.Ambar, Icons.Filled.CloudUpload)
                    }
                    if (anulado != null) Etiqueta("Anulado: ${anulado.motivo}", eco.anulado, eco.anuladoFondo, Icons.Filled.Block)
                }
                p.errorSincronizacion?.let {
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = eco.anulado, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("El servidor lo rechazó: $it", style = MaterialTheme.typography.bodySmall, color = eco.anulado)
                    }
                }
            }

            if (ui.puedeCorregir || ui.puedeAnular || ui.puedeDescartar) {
                HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ui.puedeCorregir) AccionTexto("Corregir", Icons.Filled.Edit, MaterialTheme.colorScheme.primary, alCorregir)
                    // "Anular" solo aparece para quien tiene PESAJE_ANULAR.
                    if (ui.puedeAnular) AccionTexto("Anular", Icons.Filled.Block, eco.anulado, alAnular)
                    if (ui.puedeDescartar) AccionTexto("Descartar", Icons.Filled.DeleteOutline, eco.anulado, alDescartar)
                }
            }
        }
    }
}

@Composable
private fun AccionTexto(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color, alPulsar: () -> Unit) {
    TextButton(onClick = alPulsar) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SinPesajes(puedeRegistrar: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.eco.trazo, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(8.dp))
        Text("Todavía no hay pesajes aquí", style = MaterialTheme.typography.titleMedium)
        Text(
            if (puedeRegistrar) "Pesa lo recogido y regístralo con el botón de abajo." else "Aún nadie registró pesajes en esta parada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.eco.textoSecundario
        )
    }
}

@Composable
private fun DialogoAnular(alConfirmar: (String) -> Unit, alCancelar: () -> Unit) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.eco.anulado) },
        title = { Text("Anular pesaje") },
        text = {
            Column {
                Text("El pesaje no se borra: queda marcado como anulado con el motivo que escribas.")
                OutlinedTextField(
                    value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(motivo) }, enabled = motivo.isNotBlank()) {
                Text("Anular pesaje", color = MaterialTheme.eco.anulado)
            }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

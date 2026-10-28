package pe.upeu.ecorecicla.presentation.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.componentes.BannerMensajes
import pe.upeu.ecorecicla.presentation.componentes.BotonPrincipal
import pe.upeu.ecorecicla.presentation.componentes.CifraKg
import pe.upeu.ecorecicla.presentation.componentes.iconoMaterial
import pe.upeu.ecorecicla.presentation.theme.Paleta
import pe.upeu.ecorecicla.presentation.theme.eco

private val MATERIALES = listOf("Carton", "Plastico", "Papel", "Vidrio", "Metal")

@Composable
fun FormPesajeScreen(
    puntoId: Long,
    rutaId: Long,
    pesajeId: Long?,
    alTerminar: (String) -> Unit,
    alVolver: () -> Unit,
    vm: FormPesajeViewModel = koinViewModel()
) {
    LaunchedEffect(puntoId, pesajeId) { vm.iniciar(puntoId, rutaId, pesajeId) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminado) { estado.terminado?.let(alTerminar) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(Paleta.Bosque).statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = alVolver) { Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = Color.White) }
            Text(
                if (estado.esCorreccion) "Corregir pesaje" else "Nuevo pesaje",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BannerMensajes(null, estado.error, alReintentar = vm::guardar, alCerrar = {})

            Seccion("Qué se recogió") {
                // Rejilla de materiales: objetivos grandes, faciles de pulsar con guantes.
                MATERIALES.chunked(3).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        fila.forEach { m ->
                            BotonMaterial(m, seleccionado = estado.material.equals(m, ignoreCase = true),
                                alPulsar = { vm.cambiarMaterial(m) }, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                CampoTexto("Material (elige arriba o escribe otro)", estado.material, vm::cambiarMaterial, estado.errores["material"])
            }

            Seccion("Peso en la balanza") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoPeso("Peso bruto", estado.pesoBruto, vm::cambiarBruto, estado.errores["pesoBruto"], Modifier.weight(1f))
                    CampoPeso("Tara", estado.tara, vm::cambiarTara, estado.errores["tara"], Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                ResultadoNeto(estado.netoEstimado)
            }

            Seccion("Observación") {
                CampoTexto("Opcional: estado del material, acceso, etc.", estado.observacion, vm::cambiarObservacion,
                    estado.errores["observacion"])
            }
        }

        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                BotonPrincipal(
                    texto = if (estado.esCorreccion) "Guardar corrección" else "Guardar pesaje",
                    icono = Icons.Filled.Save,
                    alPulsar = vm::guardar,
                    cargando = estado.enviando
                )
                Text(
                    "Sin señal se guarda en el equipo y se envía al sincronizar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.eco.textoSecundario,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        contenido()
    }
}

@Composable
private fun BotonMaterial(m: String, seleccionado: Boolean, alPulsar: () -> Unit, modifier: Modifier = Modifier) {
    val fondo = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val texto = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = alPulsar,
        shape = MaterialTheme.shapes.medium,
        color = fondo,
        modifier = modifier.height(72.dp).border(
            1.dp, if (seleccionado) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(iconoMaterial(m), contentDescription = null, tint = if (seleccionado) texto else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(m, style = MaterialTheme.typography.labelLarge, color = texto)
        }
    }
}

@Composable
private fun CampoPeso(etiqueta: String, valor: String, alCambiar: (String) -> Unit, error: String?, modifier: Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        suffix = { Text("kg") },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        colors = campoColores(),
        modifier = modifier
    )
}

@Composable
private fun CampoTexto(etiqueta: String, valor: String, alCambiar: (String) -> Unit, error: String?) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = MaterialTheme.shapes.medium,
        colors = campoColores(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun campoColores() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

/** Resultado en vivo: bruto menos tara. Es solo una guia; el neto oficial lo calcula el servidor (RN-03). */
@Composable
private fun ResultadoNeto(neto: Double?) {
    val eco = MaterialTheme.eco
    val valido = neto != null && neto > 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                when {
                    neto == null -> MaterialTheme.colorScheme.surfaceVariant
                    valido -> eco.completadoFondo
                    else -> eco.anuladoFondo
                },
                MaterialTheme.shapes.medium
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Peso neto", style = MaterialTheme.typography.titleSmall)
            Text(
                when {
                    neto == null -> "Ingresa el bruto y la tara"
                    valido -> "Estimado; el servidor confirma el valor"
                    else -> "La tara debe ser menor que el bruto"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (neto != null && !valido) eco.anulado else eco.textoSecundario
            )
        }
        Spacer(Modifier.width(12.dp))
        CifraKg(
            neto ?: 0.0,
            MaterialTheme.typography.displaySmall,
            color = when {
                neto == null -> eco.textoSecundario
                valido -> MaterialTheme.colorScheme.primary
                else -> eco.anulado
            }
        )
    }
}

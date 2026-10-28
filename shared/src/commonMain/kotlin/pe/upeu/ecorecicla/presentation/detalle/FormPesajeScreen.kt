package pe.upeu.ecorecicla.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.componentes.BannerMensajes
import pe.upeu.ecorecicla.presentation.kg

private val MATERIALES = listOf("Carton", "Plastico", "Papel", "Vidrio", "Metal")

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esCorreccion) "Corregir pesaje" else "Registrar pesaje") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        }
    ) { relleno ->
        Column(
            Modifier.padding(relleno).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BannerMensajes(null, estado.error, alReintentar = vm::guardar, alCerrar = {})

            Text("Material", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MATERIALES.forEach { m ->
                    FilterChip(selected = estado.material == m, onClick = { vm.cambiarMaterial(m) }, label = { Text(m) })
                }
            }
            Campo("Material", estado.material, vm::cambiarMaterial, estado.errores["material"])
            Campo("Peso bruto (kg)", estado.pesoBruto, vm::cambiarBruto, estado.errores["pesoBruto"], numerico = true)
            Campo("Tara del contenedor (kg)", estado.tara, vm::cambiarTara, estado.errores["tara"], numerico = true)
            Campo("Observacion (opcional)", estado.observacion, vm::cambiarObservacion, estado.errores["observacion"])

            estado.netoEstimado?.let {
                Text("Neto estimado: ${kg(it)} (el valor definitivo lo calcula el servidor)",
                    style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = vm::guardar, enabled = !estado.enviando, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (estado.enviando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar")
                }
            }
            Text("Sin conexion el pesaje se guarda en el equipo y se envia al sincronizar.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, alCambiar: (String) -> Unit, error: String?, numerico: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth()
    )
}

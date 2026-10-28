package pe.upeu.ecorecicla.presentation.sesion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.componentes.BotonPrincipal
import pe.upeu.ecorecicla.presentation.componentes.MarcaEcoRutas
import pe.upeu.ecorecicla.presentation.theme.Paleta
import pe.upeu.ecorecicla.presentation.theme.eco

@Composable
fun LoginScreen(aviso: String?, vm: LoginViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var verClave by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Paleta.Bosque).imePadding()) {
        // Marca: el mismo trazo de ruta y pin del icono de la app
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 40.dp)) {
            MarcaEcoRutas(Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("EcoRutas", style = MaterialTheme.typography.displayLarge, color = Color.White)
            Text(
                "Registra los pesajes de tu ruta, con o sin señal.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f)
            )
        }

        // Formulario sobre una hoja blanca
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .navigationBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Inicia tu turno", style = MaterialTheme.typography.headlineSmall)
                Text("Usa el usuario y la contraseña que te dio EcoRecicla.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.eco.textoSecundario)

                val colores = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
                OutlinedTextField(
                    value = estado.usuario,
                    onValueChange = vm::cambiarUsuario,
                    label = { Text("Usuario") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    isError = estado.errorUsuario != null,
                    supportingText = estado.errorUsuario?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.medium,
                    colors = colores,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.clave,
                    onValueChange = vm::cambiarClave,
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { verClave = !verClave }) {
                            Icon(if (verClave) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (verClave) "Ocultar contraseña" else "Mostrar contraseña")
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = estado.errorClave != null,
                    supportingText = estado.errorClave?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.ingresar() }),
                    shape = MaterialTheme.shapes.medium,
                    colors = colores,
                    modifier = Modifier.fillMaxWidth()
                )

                (estado.mensaje ?: aviso)?.let { texto -> AvisoLogin(texto) }

                Spacer(Modifier.height(4.dp))
                BotonPrincipal(texto = "Ingresar", alPulsar = vm::ingresar, cargando = estado.cargando)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AvisoLogin(texto: String) {
    val sinRed = texto.contains("conexión", ignoreCase = true) || texto.contains("respondió", ignoreCase = true)
    val eco = MaterialTheme.eco
    Row(
        Modifier.fillMaxWidth()
            .background(if (sinRed) eco.pendienteFondo else eco.anuladoFondo, MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (sinRed) Icons.Filled.WifiOff else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = if (sinRed) Color(0xFF6B4A00) else eco.anulado
        )
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

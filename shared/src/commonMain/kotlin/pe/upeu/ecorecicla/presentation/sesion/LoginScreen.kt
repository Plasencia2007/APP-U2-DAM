package pe.upeu.ecorecicla.presentation.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(aviso: String?, vm: LoginViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("EcoRutas", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Pesajes en ruta · EcoRecicla", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (aviso != null && estado.mensaje == null) {
                Aviso(aviso)
            }

            OutlinedTextField(
                value = estado.usuario,
                onValueChange = vm::cambiarUsuario,
                label = { Text("Usuario") },
                singleLine = true,
                isError = estado.errorUsuario != null,
                supportingText = estado.errorUsuario?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = estado.clave,
                onValueChange = vm::cambiarClave,
                label = { Text("Contrasena") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = estado.errorClave != null,
                supportingText = estado.errorClave?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            estado.mensaje?.let { Aviso(it) }

            Button(onClick = vm::ingresar, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (estado.cargando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Ingresar")
                }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(texto, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

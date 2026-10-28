package pe.upeu.ecorecicla.presentation.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.componentes.BotonPrincipal
import pe.upeu.ecorecicla.presentation.componentes.Etiqueta
import pe.upeu.ecorecicla.presentation.theme.Paleta
import pe.upeu.ecorecicla.presentation.theme.eco

/** Cada privilegio contado en el idioma del operario. */
private val DESCRIPCIONES: Map<String, Pair<String, ImageVector>> = mapOf(
    "PUNTO_LEER" to ("Ver la ruta y los puntos de acopio" to Icons.Filled.Map),
    "PESAJE_LEER" to ("Ver los pesajes de cada parada" to Icons.Filled.Visibility),
    "PESAJE_CREAR" to ("Registrar pesajes nuevos" to Icons.Filled.PostAdd),
    "PESAJE_EDITAR" to ("Corregir pesajes" to Icons.Filled.Edit),
    "PESAJE_EDITAR_TODOS" to ("Corregir pesajes de cualquier operario" to Icons.Filled.EditNote),
    "PESAJE_ANULAR" to ("Anular pesajes" to Icons.Filled.Block),
    "RUTA_CERRAR" to ("Cerrar la ruta del día" to Icons.Filled.Lock),
    "PUNTO_GESTIONAR" to ("Gestionar puntos de acopio y rutas" to Icons.Filled.Storefront)
)

@Composable
fun PerfilScreen(alVolver: () -> Unit, vm: PerfilViewModel = koinViewModel()) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val cerrando by vm.cerrando.collectAsStateWithLifecycle()
    var confirmar by remember { mutableStateOf(false) }
    val s = sesion ?: return
    val eco = MaterialTheme.eco

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().background(Paleta.Bosque).statusBarsPadding().padding(start = 4.dp, end = 20.dp, bottom = 24.dp)) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Row(Modifier.padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val iniciales = s.nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
                Box(Modifier.size(64.dp).background(Paleta.Ambar, CircleShape), contentAlignment = Alignment.Center) {
                    Text(iniciales, style = MaterialTheme.typography.headlineSmall, color = Paleta.Carbon)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(s.nombre, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text("Usuario ${s.usuario}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.height(6.dp))
                    Etiqueta(s.rol.lowercase().replaceFirstChar { it.uppercase() }, Paleta.BosqueProfundo, Color(0xFF7FD3A8),
                        Icons.Filled.Shield)
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Lo que puedes hacer", style = MaterialTheme.typography.titleLarge)
            Text("Tus permisos vienen de tu rol y el servidor los vuelve a comprobar en cada operación.",
                style = MaterialTheme.typography.bodyMedium, color = eco.textoSecundario, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Column {
                    val lista = s.privilegios.sorted()
                    lista.forEachIndexed { i, codigo ->
                        val (texto, icono) = DESCRIPCIONES[codigo] ?: (codigo to Icons.Filled.Shield)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(texto, style = MaterialTheme.typography.bodyLarge)
                                Text(codigo, style = MaterialTheme.typography.labelSmall, color = eco.textoSecundario)
                            }
                        }
                        if (i < lista.lastIndex) HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPrincipal(
                texto = if (cerrando) "Cerrando sesión" else "Terminar turno y cerrar sesión",
                icono = Icons.AutoMirrored.Filled.Logout,
                alPulsar = { confirmar = true },
                cargando = cerrando,
                contenedor = eco.anulado,
                contenido = Color.White
            )
            Text("Se borra tu token y los datos de este equipo para que el siguiente turno empiece limpio.",
                style = MaterialTheme.typography.bodySmall, color = eco.textoSecundario)
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Terminar turno") },
            text = { Text("Si hay señal, primero se envían los pesajes pendientes. Lo que no se pueda enviar se perderá.") },
            confirmButton = { TextButton(onClick = { confirmar = false; vm.salir() }) { Text("Cerrar sesión", color = eco.anulado) } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Seguir trabajando") } }
        )
    }
}

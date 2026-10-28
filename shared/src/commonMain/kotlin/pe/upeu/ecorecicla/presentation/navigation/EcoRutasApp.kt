package pe.upeu.ecorecicla.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoScreen
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel
import pe.upeu.ecorecicla.presentation.detalle.FormPesajeScreen
import pe.upeu.ecorecicla.presentation.listado.RutaScreen
import pe.upeu.ecorecicla.presentation.perfil.PerfilScreen
import pe.upeu.ecorecicla.presentation.sesion.EstadoSesion
import pe.upeu.ecorecicla.presentation.sesion.LoginScreen
import pe.upeu.ecorecicla.presentation.sesion.SesionViewModel
import pe.upeu.ecorecicla.presentation.theme.TemaEcoRutas

/** Raiz de la interfaz: sin sesion muestra el login; con sesion, el destino actual de la pila. */
@Composable
fun EcoRutasApp() {
    TemaEcoRutas {
        Surface(Modifier.fillMaxSize()) {
            val sesionVm: SesionViewModel = koinViewModel()
            val navegador: Navegador = koinInject()
            val estado by sesionVm.estado.collectAsStateWithLifecycle()
            val pila by navegador.pila.collectAsStateWithLifecycle()

            when (val e = estado) {
                EstadoSesion.Verificando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is EstadoSesion.SinSesion -> {
                    LaunchedEffect(Unit) { navegador.reiniciar() }
                    LoginScreen(aviso = e.aviso)
                }
                is EstadoSesion.Activa -> AreaProtegida(pila.last(), navegador)
            }
        }
    }
}

@Composable
private fun AreaProtegida(destino: Destino, navegador: Navegador) {
    when (destino) {
        Destino.Ruta -> RutaScreen(
            alAbrirPunto = { navegador.ir(Destino.Detalle(it)) },
            alAbrirPerfil = { navegador.ir(Destino.Perfil) }
        )
        is Destino.Detalle -> DetallePuntoScreen(
            puntoId = destino.puntoId,
            alVolver = { navegador.atras() },
            alRegistrar = { rutaId -> navegador.ir(Destino.Formulario(destino.puntoId, rutaId)) },
            alCorregir = { rutaId, pesajeId -> navegador.ir(Destino.Formulario(destino.puntoId, rutaId, pesajeId)) }
        )
        is Destino.Formulario -> {
            val detalleVm: DetallePuntoViewModel = koinViewModel()
            FormPesajeScreen(
                puntoId = destino.puntoId,
                rutaId = destino.rutaId,
                pesajeId = destino.pesajeId,
                alTerminar = { mensaje ->
                    navegador.atras()
                    detalleVm.informar(mensaje)
                },
                alVolver = { navegador.atras() }
            )
        }
        Destino.Perfil -> PerfilScreen(alVolver = { navegador.atras() })
    }
}

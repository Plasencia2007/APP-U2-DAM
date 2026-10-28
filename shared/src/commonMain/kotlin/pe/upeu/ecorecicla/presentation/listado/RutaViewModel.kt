package pe.upeu.ecorecicla.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoRuta
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Ruta
import pe.upeu.ecorecicla.domain.repository.MonitorDeRed
import pe.upeu.ecorecicla.domain.usecase.CerrarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.upeu.ecorecicla.domain.usecase.PermisosUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.MensajeError
import pe.upeu.ecorecicla.presentation.aMensaje

data class RutaUiState(
    val cargando: Boolean = true,
    val ruta: Ruta? = null,
    val busqueda: String = "",
    val ultimaSincronizacion: String? = null,
    val sincronizando: Boolean = false,
    val conectado: Boolean = true,
    val puedeCerrarRuta: Boolean = false,
    val mensaje: String? = null,
    val error: MensajeError? = null
) {
    val vacia: Boolean get() = !cargando && (ruta == null || ruta.puntos.isEmpty())
    val pesados: Int get() = ruta?.puntos?.count { it.atendido } ?: 0
}

/**
 * Pantalla principal. Lee la ruta de la BASE LOCAL (flujo observable): sin senal sigue mostrando datos.
 * La red solo se usa para sincronizar, a mano o automaticamente al recuperar la conexion.
 */
class RutaViewModel(
    private val observarRuta: ObservarRutaUseCase,
    observarUltimaSincronizacion: ObservarUltimaSincronizacionUseCase,
    private val sincronizarUseCase: SincronizarUseCase,
    private val cerrarRutaUseCase: CerrarRutaUseCase,
    private val observarSesion: ObservarSesionUseCase,
    private val permisos: PermisosUseCase,
    private val red: MonitorDeRed
) : ViewModel() {

    private val _estado = MutableStateFlow(RutaUiState())
    val estado: StateFlow<RutaUiState> = _estado.asStateFlow()

    private val busqueda = MutableStateFlow("")

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            busqueda.flatMapLatest { observarRuta(it) }.collect { ruta ->
                _estado.update { it.copy(cargando = false, ruta = ruta) }
            }
        }
        viewModelScope.launch {
            observarUltimaSincronizacion().collect { fecha -> _estado.update { it.copy(ultimaSincronizacion = fecha) } }
        }
        viewModelScope.launch {
            observarSesion().collect { s -> _estado.update { it.copy(puedeCerrarRuta = permisos.puedeCerrarRuta(s)) } }
        }
        // Sincronizacion automatica al recuperar la conexion (transicion sin red -> con red).
        viewModelScope.launch {
            var anterior: Boolean? = null
            red.conectado.collect { conectado ->
                _estado.update { it.copy(conectado = conectado) }
                if (anterior == false && conectado) sincronizar()
                anterior = conectado
            }
        }
        // Primera carga de cada turno: al iniciar sesion se baja la ruta del dia.
        viewModelScope.launch {
            observarSesion().filterNotNull().map { it.usuario }.distinctUntilChanged().collect {
                if (red.conectado.value) sincronizar()
            }
        }
    }

    fun buscar(texto: String) {
        busqueda.value = texto
        _estado.update { it.copy(busqueda = texto) }
    }

    fun sincronizar() {
        if (_estado.value.sincronizando || observarSesion().value == null) return
        _estado.update { it.copy(sincronizando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            when (val r = sincronizarUseCase()) {
                is Resultado.Exito -> _estado.update {
                    val s = r.valor
                    val rechazo = if (s.rechazados > 0) " · ${s.rechazados} rechazados (ver detalle)" else ""
                    it.copy(sincronizando = false, mensaje = "Sincronizado: ${s.enviados} enviados, ${s.recibidos} recibidos$rechazo")
                }
                is Resultado.Fallo -> _estado.update { it.copy(sincronizando = false, error = r.error.aMensaje()) }
            }
        }
    }

    fun cerrarRuta() {
        val ruta = _estado.value.ruta ?: return
        if (ruta.estado == EstadoRuta.CERRADA) return
        viewModelScope.launch {
            when (val r = cerrarRutaUseCase(ruta.id)) {
                is Resultado.Exito -> _estado.update { it.copy(mensaje = "Ruta cerrada correctamente") }
                is Resultado.Fallo -> {
                    _estado.update { it.copy(error = r.error.aMensaje()) }
                    if (r.error is ErrorApi.NoEncontrado) sincronizar()
                }
            }
        }
    }

    fun descartarMensajes() = _estado.update { it.copy(mensaje = null, error = null) }
}

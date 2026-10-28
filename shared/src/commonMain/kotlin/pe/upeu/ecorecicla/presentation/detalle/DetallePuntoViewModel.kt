package pe.upeu.ecorecicla.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoRuta
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistroPesaje
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.usecase.AnularPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.DescartarPendienteUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPesajesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntoUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.PermisosUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.MensajeError
import pe.upeu.ecorecicla.presentation.aMensaje

/** Un pesaje con las acciones que la interfaz puede ofrecer sobre el. */
data class PesajeUi(
    val pesaje: Pesaje,
    val puedeCorregir: Boolean,
    val puedeAnular: Boolean,
    val puedeDescartar: Boolean
)

data class DetalleUiState(
    val cargando: Boolean = true,
    val punto: PuntoAcopio? = null,
    val rutaId: Long = 0,
    val rutaCerrada: Boolean = false,
    val pesajes: List<PesajeUi> = emptyList(),
    val puedeRegistrar: Boolean = false,
    val procesando: Boolean = false,
    val mensaje: String? = null,
    val error: MensajeError? = null
)

class DetallePuntoViewModel(
    private val observarPunto: ObservarPuntoUseCase,
    private val observarPesajes: ObservarPesajesUseCase,
    private val observarRuta: ObservarRutaUseCase,
    private val observarSesion: ObservarSesionUseCase,
    private val permisos: PermisosUseCase,
    private val anularPesaje: AnularPesajeUseCase,
    private val descartarPendiente: DescartarPendienteUseCase,
    private val sincronizar: SincronizarUseCase,
    private val reloj: Reloj
) : ViewModel() {

    private val puntoId = MutableStateFlow<Long?>(null)
    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            puntoId.filterNotNull().flatMapLatest { id ->
                combine(observarPunto(id), observarPesajes(id), observarRuta(), observarSesion()) { punto, pesajes, ruta, sesion ->
                    val cerrada = ruta?.estado == EstadoRuta.CERRADA
                    val hoy = reloj.hoy()
                    // Las decisiones de privilegio se toman aqui (y en el servicio), no en el composable.
                    DetalleUiState(
                        cargando = false,
                        punto = punto,
                        rutaId = ruta?.id ?: 0,
                        rutaCerrada = cerrada,
                        puedeRegistrar = permisos.puedeRegistrar(sesion) && !cerrada && ruta != null,
                        pesajes = pesajes.map { p ->
                            PesajeUi(
                                pesaje = p,
                                puedeCorregir = !cerrada && permisos.puedeCorregir(sesion, p, hoy),
                                puedeAnular = !cerrada && p.id > 0 && permisos.puedeAnular(sesion, p),
                                puedeDescartar = p.pendiente && p.id < 0
                            )
                        }
                    )
                }
            }.collect { nuevo ->
                _estado.update { nuevo.copy(mensaje = it.mensaje, error = it.error, procesando = it.procesando) }
            }
        }
    }

    fun seleccionar(id: Long) {
        if (puntoId.value != id) {
            _estado.value = DetalleUiState()
            puntoId.value = id
        }
    }

    fun anular(pesajeId: Long, motivo: String) {
        _estado.update { it.copy(procesando = true, mensaje = null, error = null) }
        viewModelScope.launch {
            when (val r = anularPesaje(pesajeId, motivo)) {
                is Resultado.Exito -> _estado.update {
                    it.copy(procesando = false, mensaje = when (r.valor) {
                        is RegistroPesaje.Enviado -> "Pesaje anulado"
                        is RegistroPesaje.Pendiente -> "Anulacion guardada sin conexion: se enviara al sincronizar"
                    })
                }
                is Resultado.Fallo -> {
                    _estado.update { it.copy(procesando = false, error = r.error.aMensaje()) }
                    // 404: se informa y se refresca el listado; no se ofrece reintentar.
                    if (r.error is ErrorApi.NoEncontrado) sincronizar()
                }
            }
        }
    }

    fun descartar(pesajeId: Long) {
        viewModelScope.launch {
            descartarPendiente(pesajeId)
            _estado.update { it.copy(mensaje = "Pesaje pendiente descartado") }
        }
    }

    /** Mensaje que deja el formulario al volver (enviado o guardado como pendiente). */
    fun informar(mensaje: String) = _estado.update { it.copy(mensaje = mensaje, error = null) }

    fun descartarMensajes() =_estado.update { it.copy(mensaje = null, error = null) }
}

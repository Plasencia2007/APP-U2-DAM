package pe.upeu.ecorecicla.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.RegistroPesaje
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.usecase.CorregirPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.ObtenerPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.RegistrarPesajeUseCase
import pe.upeu.ecorecicla.presentation.MensajeError
import pe.upeu.ecorecicla.presentation.aMensaje
import pe.upeu.ecorecicla.presentation.numeroKg

data class FormPesajeUiState(
    val puntoId: Long = 0,
    val rutaId: Long = 0,
    val pesajeId: Long? = null,
    val material: String = "",
    val pesoBruto: String = "",
    val tara: String = "",
    val observacion: String = "",
    /** SC-C: humedad estimada en % (opcional). */
    val humedad: String = "",
    /** Mensaje bajo cada campo: de la validacion local o de validationErrors del servicio. */
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false,
    val error: MensajeError? = null,
    /** Mensaje final; cuando no es null la pantalla vuelve al detalle. */
    val terminado: String? = null
) {
    val esCorreccion: Boolean get() = pesajeId != null

    /** Solo informativo: el neto definitivo lo calcula el servicio. */
    val netoEstimado: Double?
        get() {
            val b = pesoBruto.replace(',', '.').toDoubleOrNull() ?: return null
            val t = tara.replace(',', '.').toDoubleOrNull() ?: return null
            return b - t
        }
}

class FormPesajeViewModel(
    private val registrar: RegistrarPesajeUseCase,
    private val corregir: CorregirPesajeUseCase,
    private val obtener: ObtenerPesajeUseCase
) : ViewModel() {

    private val _estado = MutableStateFlow(FormPesajeUiState())
    val estado: StateFlow<FormPesajeUiState> = _estado.asStateFlow()

    fun iniciar(puntoId: Long, rutaId: Long, pesajeId: Long?) {
        _estado.value = FormPesajeUiState(puntoId = puntoId, rutaId = rutaId, pesajeId = pesajeId)
        if (pesajeId != null) {
            viewModelScope.launch {
                obtener(pesajeId)?.let { p ->
                    _estado.update {
                        it.copy(material = p.material, pesoBruto = p.pesoBruto.toString(), tara = p.tara.toString(),
                            observacion = p.observacion.orEmpty(),
                            humedad = p.humedadEstimada?.let { numeroKg(it) }.orEmpty())
                    }
                }
            }
        }
    }

    fun cambiarMaterial(v: String) = _estado.update { it.copy(material = v, errores = it.errores - "material") }
    fun cambiarBruto(v: String) = _estado.update { it.copy(pesoBruto = v, errores = it.errores - "pesoBruto") }
    fun cambiarTara(v: String) = _estado.update { it.copy(tara = v, errores = it.errores - "tara") }
    fun cambiarHumedad(v: String) = _estado.update { it.copy(humedad = v, errores = it.errores - "humedadEstimada") }
    fun cambiarObservacion(v: String) = _estado.update { it.copy(observacion = v, errores = it.errores - "observacion") }

    fun guardar() {
        val s = _estado.value
        if (s.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val r = if (s.pesajeId == null) {
                registrar(s.puntoId, s.rutaId, s.material, s.pesoBruto, s.tara, s.observacion, s.humedad)
            } else {
                corregir(s.pesajeId, s.material, s.pesoBruto, s.tara, s.observacion, s.humedad)
            }
            when (r) {
                is Resultado.Exito -> _estado.update {
                    it.copy(enviando = false, terminado = when (val v = r.valor) {
                        is RegistroPesaje.Enviado -> "Pesaje guardado en el servidor (id ${v.pesaje.id}, neto ${numeroKg(v.pesaje.pesoNeto)} kg)"
                        is RegistroPesaje.Pendiente -> "Sin conexión: pesaje guardado en el equipo como pendiente (id ${v.pesaje.id})"
                    })
                }
                is Resultado.Fallo -> _estado.update {
                    val e = r.error
                    if (e is ErrorApi.Validacion && e.errores.isNotEmpty()) {
                        // 400: cada mensaje del servidor va bajo el campo que lo origino.
                        it.copy(enviando = false, errores = e.errores)
                    } else {
                        it.copy(enviando = false, error = e.aMensaje())
                    }
                }
            }
        }
    }
}

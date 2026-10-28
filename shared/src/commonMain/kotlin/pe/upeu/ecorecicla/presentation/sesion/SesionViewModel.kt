package pe.upeu.ecorecicla.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.RecuperarSesionUseCase

sealed interface EstadoSesion {
    data object Verificando : EstadoSesion
    data class SinSesion(val aviso: String? = null) : EstadoSesion
    data class Activa(val sesion: Sesion) : EstadoSesion
}

/**
 * Decide si se muestra el inicio de sesion o el area protegida. Al abrir la app recupera
 * el token guardado: si sigue vigente entra directo; si vencio vuelve al inicio de sesion.
 */
class SesionViewModel(
    recuperarSesion: RecuperarSesionUseCase,
    observarSesion: ObservarSesionUseCase
) : ViewModel() {

    private val verificado = MutableStateFlow(false)

    val estado: StateFlow<EstadoSesion> = combine(
        verificado, observarSesion(), observarSesion.vencida()
    ) { listo, sesion, vencida ->
        when {
            !listo -> EstadoSesion.Verificando
            sesion != null -> EstadoSesion.Activa(sesion)
            vencida -> EstadoSesion.SinSesion("Su sesion vencio. Inicie sesion nuevamente.")
            else -> EstadoSesion.SinSesion()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Verificando)

    init {
        viewModelScope.launch {
            recuperarSesion()
            verificado.value = true
        }
    }
}

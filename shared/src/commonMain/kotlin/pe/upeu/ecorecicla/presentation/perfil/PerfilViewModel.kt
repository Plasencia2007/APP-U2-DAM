package pe.upeu.ecorecicla.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.MonitorDeRed
import pe.upeu.ecorecicla.domain.usecase.CerrarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase

class PerfilViewModel(
    observarSesion: ObservarSesionUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val sincronizar: SincronizarUseCase,
    private val red: MonitorDeRed
) : ViewModel() {

    val sesion: StateFlow<Sesion?> = observarSesion()

    private val _cerrando = MutableStateFlow(false)
    val cerrando: StateFlow<Boolean> = _cerrando.asStateFlow()

    /**
     * Cierre de sesion del turno: si hay red, primero intenta enviar lo pendiente;
     * luego borra el token del almacenamiento seguro y vacia la base local.
     */
    fun salir() {
        if (_cerrando.value) return
        _cerrando.value = true
        viewModelScope.launch {
            if (red.conectado.value) sincronizar()
            cerrarSesion()
            _cerrando.value = false
        }
    }
}

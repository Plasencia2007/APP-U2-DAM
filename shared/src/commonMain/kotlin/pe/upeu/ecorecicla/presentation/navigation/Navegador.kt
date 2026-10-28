package pe.upeu.ecorecicla.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Destinos del area protegida. El inicio de sesion se decide por el estado de la sesion. */
sealed interface Destino {
    data object Ruta : Destino
    data class Detalle(val puntoId: Long) : Destino
    data class Formulario(val puntoId: Long, val rutaId: Long, val pesajeId: Long? = null) : Destino
    data object Perfil : Destino
}

/** Pila de navegacion simple, compartida por Koin y manejada por el boton Atras de Android. */
class Navegador {
    private val _pila = MutableStateFlow<List<Destino>>(listOf(Destino.Ruta))
    val pila: StateFlow<List<Destino>> = _pila.asStateFlow()

    fun ir(destino: Destino) {
        _pila.value = _pila.value + destino
    }

    /** Devuelve false si ya no hay a donde volver (la actividad se cierra). */
    fun atras(): Boolean {
        if (_pila.value.size <= 1) return false
        _pila.value = _pila.value.dropLast(1)
        return true
    }

    fun reiniciar() {
        _pila.value = listOf(Destino.Ruta)
    }
}

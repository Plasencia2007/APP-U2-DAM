package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Ruta
import pe.upeu.ecorecicla.domain.repository.RutaRepository

/** La ruta del dia se observa SIEMPRE desde la base local; la red solo alimenta esa base. */
class ObservarRutaUseCase(private val rutas: RutaRepository) {
    operator fun invoke(busqueda: String = ""): Flow<Ruta?> = rutas.observarRuta(busqueda.trim())
}

class ObservarPuntoUseCase(private val rutas: RutaRepository) {
    operator fun invoke(puntoId: Long): Flow<PuntoAcopio?> = rutas.observarPunto(puntoId)
}

class ObservarUltimaSincronizacionUseCase(private val rutas: RutaRepository) {
    operator fun invoke(): Flow<String?> = rutas.observarUltimaSincronizacion()
}

class CerrarRutaUseCase(private val rutas: RutaRepository) {
    suspend operator fun invoke(rutaId: Long): Resultado<Unit> = rutas.cerrarRuta(rutaId)
}

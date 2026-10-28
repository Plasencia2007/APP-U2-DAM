package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.ResumenSincronizacion
import pe.upeu.ecorecicla.domain.repository.SincronizacionRepository

/**
 * Sincronizacion offline-first: PRIMERO sube lo pendiente y DESPUES baja lo actualizado.
 *
 * Si se invirtiera el orden, la bajada reemplazaria la ruta local con la del servidor, que todavia
 * no conoce los pesajes registrados sin senal; el punto apareceria como "sin pesar" y una correccion
 * hecha sin red seria pisada por el valor viejo del servidor.
 */
class SincronizarUseCase(private val sincronizacion: SincronizacionRepository) {

    // Evita que la sincronizacion automatica y la manual se ejecuten a la vez.
    private val enCurso = Mutex()

    suspend operator fun invoke(): Resultado<ResumenSincronizacion> = enCurso.withLock {
        val subida = when (val r = sincronizacion.subirPendientes()) {
            is Resultado.Exito -> r.valor
            is Resultado.Fallo -> return@withLock r
        }
        val recibidos = when (val r = sincronizacion.bajarActualizaciones()) {
            is Resultado.Exito -> r.valor
            is Resultado.Fallo -> return@withLock r
        }
        sincronizacion.marcarSincronizado()
        Resultado.Exito(ResumenSincronizacion(subida.enviados, recibidos, subida.rechazados))
    }
}

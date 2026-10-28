package pe.upeu.ecorecicla.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.Metadatos
import pe.upeu.ecorecicla.data.local.aDominio
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.db.EcoDb
import pe.upeu.ecorecicla.domain.model.EstadoRuta
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Ruta
import pe.upeu.ecorecicla.domain.model.map
import pe.upeu.ecorecicla.domain.repository.RutaRepository

/** Offline-first: lo que la pantalla observa sale de la base local; la red solo la actualiza. */
class RutaRepositoryImpl(
    private val db: EcoDb,
    private val api: EcoApi,
    private val io: CoroutineDispatcher
) : RutaRepository {

    private val q get() = db.ecoDbQueries

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observarRuta(busqueda: String, soloPendientes: Boolean): Flow<Ruta?> =
        q.rutaActual().asFlow().mapToOneOrNull(io).flatMapLatest { fila ->
            if (fila == null) {
                flowOf(null)
            } else {
                combine(
                    q.puntosDeRuta(fila.id, busqueda, if (soloPendientes) 1L else 0L).asFlow().mapToList(io),
                    q.puntosDeRuta(fila.id, "", 0L).asFlow().mapToList(io),
                    q.netoTotalRuta(fila.id).asFlow().mapToOne(io)
                ) { filtrados, todos, neto ->
                    Ruta(
                        id = fila.id,
                        fecha = fila.fecha,
                        estado = if (fila.estado == "CERRADA") EstadoRuta.CERRADA else EstadoRuta.ABIERTA,
                        operario = fila.operario,
                        puntos = filtrados.map { it.aDominio() },
                        netoTotal = neto,
                        avance = todos.map { it.atendido }
                    )
                }
            }
        }

    override fun observarPunto(puntoId: Long): Flow<PuntoAcopio?> =
        q.puntoPorId(puntoId).asFlow().mapToOneOrNull(io).map { it?.aDominio() }

    override fun observarUltimaSincronizacion(): Flow<String?> =
        q.leerMetadato(Metadatos.ULTIMA_SINCRONIZACION).asFlow().mapToOneOrNull(io)

    /** Cerrar la ruta solo tiene sentido en linea: el servicio aplica RN-04 y responde 409 si faltan puntos. */
    override suspend fun cerrarRuta(rutaId: Long): Resultado<Unit> {
        val resultado = api.cerrarRuta(rutaId)
        if (resultado is Resultado.Exito) {
            withContext(io) { q.cambiarEstadoRuta(resultado.valor.estado, rutaId) }
        }
        return resultado.map { }
    }
}

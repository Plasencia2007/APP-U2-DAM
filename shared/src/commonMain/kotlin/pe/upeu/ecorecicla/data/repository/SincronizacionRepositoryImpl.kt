package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.Metadatos
import pe.upeu.ecorecicla.data.local.Operacion
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.data.local.guardarPesaje
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.dto.CorreccionPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PesajePeticionDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.db.EcoDb
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Subida
import pe.upeu.ecorecicla.domain.repository.SincronizacionRepository
import pe.upeu.ecorecicla.db.Pesaje as FilaPesaje

class SincronizacionRepositoryImpl(
    private val db: EcoDb,
    private val api: EcoApi,
    private val reloj: Reloj,
    private val io: CoroutineDispatcher
) : SincronizacionRepository {

    private val q get() = db.ecoDbQueries

    /**
     * Envia cada fila pendiente en el orden en que se registro.
     * - Exito: la fila provisional se sustituye por la definitiva DENTRO DE LA MISMA TRANSACCION.
     * - Rechazo de negocio (400/403/404/409): la fila queda pendiente con el motivo y se sigue con la siguiente.
     * - Falta de red, 401 o 5xx: se detiene y se informa; nada se pierde porque la marca sigue puesta.
     */
    override suspend fun subirPendientes(): Resultado<Subida> {
        val pendientes = withContext(io) { q.pendientes().executeAsList() }
        var enviados = 0
        var rechazados = 0
        for (fila in pendientes) {
            val resultado = enviar(fila)
            when (resultado) {
                is Resultado.Exito -> enviados++
                is Resultado.Fallo -> when (resultado.error) {
                    is ErrorApi.Validacion, is ErrorApi.SinPrivilegio, is ErrorApi.NoEncontrado, is ErrorApi.Conflicto -> {
                        rechazados++
                        withContext(io) { q.marcarErrorSync(resultado.error.message, fila.id) }
                    }
                    else -> return resultado
                }
            }
        }
        return Resultado.Exito(Subida(enviados, rechazados))
    }

    private suspend fun enviar(fila: FilaPesaje): Resultado<Unit> = when (fila.operacion) {
        Operacion.CREAR -> {
            val peticion = PesajePeticionDto(fila.ruta_id, fila.material, fila.peso_bruto, fila.tara, fila.observacion,
                fila.clave_cliente)
            when (val r = api.registrarPesaje(fila.punto_id, peticion)) {
                is Resultado.Exito -> {
                    withContext(io) {
                        db.transaction {
                            q.borrarPesaje(fila.id)                                   // fila provisional (id < 0)
                            db.guardarPesaje(r.valor.aDominio(), claveCliente = fila.clave_cliente) // fila definitiva
                        }
                    }
                    Resultado.Exito(Unit)
                }
                is Resultado.Fallo -> r
            }
        }
        Operacion.EDITAR -> {
            val peticion = CorreccionPesajeDto(fila.material, fila.peso_bruto, fila.tara, fila.observacion)
            when (val r = api.corregirPesaje(fila.id, peticion)) {
                is Resultado.Exito -> {
                    withContext(io) { db.guardarPesaje(r.valor.aDominio()) }
                    Resultado.Exito(Unit)
                }
                is Resultado.Fallo -> r
            }
        }
        Operacion.ANULAR -> when (val r = api.anularPesaje(fila.id, fila.motivo_anulacion.orEmpty())) {
            is Resultado.Exito -> {
                withContext(io) { q.limpiarPendiente(fila.id) }
                Resultado.Exito(Unit)
            }
            is Resultado.Fallo -> r
        }
        else -> {
            withContext(io) { q.limpiarPendiente(fila.id) }
            Resultado.Exito(Unit)
        }
    }

    /** Trae la ruta del dia y TODOS sus pesajes (recorriendo las paginas) y reemplaza la copia local. */
    override suspend fun bajarActualizaciones(): Resultado<Int> {
        val ruta = when (val r = api.rutaDelDia()) {
            is Resultado.Exito -> r.valor
            is Resultado.Fallo -> {
                if (r.error is ErrorApi.NoEncontrado) {
                    // Sin ruta asignada hoy: se limpia lo ya sincronizado y se conservan los pendientes.
                    withContext(io) { db.transaction { q.borrarPesajesSincronizados(); q.borrarRutasExcepto(-1); q.borrarPuntosExceptoRuta(-1) } }
                    return Resultado.Exito(0)
                }
                return r
            }
        }
        val pesajes = mutableListOf<PesajeDto>()
        var pagina = 0
        while (true) {
            val r = api.pesajesDeRuta(ruta.id, pagina)
            if (r is Resultado.Fallo) return r
            val p = (r as Resultado.Exito).valor
            pesajes += p.contenido
            if (p.ultima || p.contenido.isEmpty()) break
            pagina++
        }
        withContext(io) {
            db.transaction {
                val pendientes = q.idsPendientes().executeAsList().toSet()
                q.borrarRutasExcepto(ruta.id)
                q.borrarPuntosExceptoRuta(ruta.id)
                q.borrarPuntosDeRuta(ruta.id)
                q.borrarPesajesSincronizados()
                q.insertarRuta(ruta.id, ruta.fecha, ruta.estado, ruta.operario)
                ruta.puntos.forEach {
                    q.insertarPunto(it.puntoId, ruta.id, it.razonSocial, it.direccion, it.zona, it.materialHabitual,
                        it.secuencia.toLong(), it.omitido)
                }
                // Una fila con cambios locales sin enviar no se pisa con la version del servidor.
                pesajes.filter { it.id !in pendientes }.forEach { db.guardarPesaje(it.aDominio(), claveCliente = it.claveCliente) }
            }
        }
        return Resultado.Exito(pesajes.size)
    }

    override suspend fun marcarSincronizado() {
        withContext(io) { q.guardarMetadato(Metadatos.ULTIMA_SINCRONIZACION, reloj.ahoraIso()) }
    }
}

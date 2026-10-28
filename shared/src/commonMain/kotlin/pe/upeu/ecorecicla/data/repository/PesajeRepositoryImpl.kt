package pe.upeu.ecorecicla.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.local.Operacion
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.data.local.aDominio
import pe.upeu.ecorecicla.data.local.guardarPesaje
import pe.upeu.ecorecicla.data.local.siguienteIdProvisional
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.dto.CorreccionPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PesajePeticionDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.db.EcoDb
import pe.upeu.ecorecicla.domain.model.DatosPesaje
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.RegistroPesaje
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.repository.MonitorDeRed
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import kotlin.math.round
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Repositorio offline-first de pesajes.
 * - Con red: envia al servicio y guarda la fila definitiva que devuelve.
 * - Sin red (o si la red falla a mitad): guarda la fila con pendiente = 1 e id provisional negativo.
 * - Errores de negocio (400, 403, 409...) NO se guardan: se devuelven para mostrar el motivo.
 */
class PesajeRepositoryImpl(
    private val db: EcoDb,
    private val api: EcoApi,
    private val red: MonitorDeRed,
    private val reloj: Reloj,
    private val sesion: GestorSesion,
    private val io: CoroutineDispatcher
) : PesajeRepository {

    private val q get() = db.ecoDbQueries

    override fun observarPesajes(puntoId: Long): Flow<List<Pesaje>> =
        q.pesajesDePunto(puntoId).asFlow().mapToList(io).map { filas -> filas.map { it.aDominio() } }

    override suspend fun obtener(pesajeId: Long): Pesaje? = withContext(io) {
        q.pesajePorId(pesajeId).executeAsOneOrNull()?.aDominio()
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun registrar(puntoId: Long, rutaId: Long, datos: DatosPesaje): Resultado<RegistroPesaje> {
        val clave = Uuid.random().toString()
        if (red.conectado.value) {
            val peticion = PesajePeticionDto(rutaId, datos.material, datos.pesoBruto, datos.tara, datos.observacion, clave,
                datos.humedadEstimada)
            when (val r = api.registrarPesaje(puntoId, peticion)) {
                is Resultado.Exito -> {
                    val definitivo = r.valor.aDominio()
                    withContext(io) { db.guardarPesaje(definitivo, claveCliente = clave) }
                    return Resultado.Exito(RegistroPesaje.Enviado(definitivo))
                }
                is Resultado.Fallo -> if (!r.error.esDeConectividad) return r
            }
        }
        // Sin conexion: se guarda igual, marcado como pendiente y con un id del rango negativo.
        val pendiente = withContext(io) {
            db.transactionWithResult {
                val p = Pesaje(
                    id = db.siguienteIdProvisional(),
                    puntoId = puntoId,
                    fechaHora = reloj.ahoraIso(),
                    material = datos.material,
                    pesoBruto = datos.pesoBruto,
                    tara = datos.tara,
                    pesoNeto = netoProvisional(datos), // solo para mostrar; el servicio calcula el definitivo
                    operario = sesion.sesion.value?.usuario.orEmpty(),
                    estado = EstadoPesaje.Registrado,
                    pendiente = true,
                    rutaId = rutaId,
                    observacion = datos.observacion,
                    humedadEstimada = datos.humedadEstimada
                )
                db.guardarPesaje(p, Operacion.CREAR, clave)
                p
            }
        }
        return Resultado.Exito(RegistroPesaje.Pendiente(pendiente))
    }

    override suspend fun corregir(pesajeId: Long, datos: DatosPesaje): Resultado<RegistroPesaje> {
        val actual = obtener(pesajeId) ?: return Resultado.Fallo(ErrorApi.NoEncontrado("El pesaje ya no existe"))
        // Un pesaje que aun no llego al servicio se corrige solo localmente: sigue pendiente de CREAR.
        if (actual.id < 0) {
            return Resultado.Exito(RegistroPesaje.Pendiente(guardarCorreccionLocal(actual, datos, Operacion.CREAR)))
        }
        if (red.conectado.value) {
            val peticion = CorreccionPesajeDto(datos.material, datos.pesoBruto, datos.tara, datos.observacion, datos.humedadEstimada)
            when (val r = api.corregirPesaje(pesajeId, peticion)) {
                is Resultado.Exito -> {
                    val definitivo = r.valor.aDominio()
                    withContext(io) { db.guardarPesaje(definitivo) }
                    return Resultado.Exito(RegistroPesaje.Enviado(definitivo))
                }
                is Resultado.Fallo -> if (!r.error.esDeConectividad) return r
            }
        }
        return Resultado.Exito(RegistroPesaje.Pendiente(guardarCorreccionLocal(actual, datos, Operacion.EDITAR)))
    }

    override suspend fun anular(pesajeId: Long, motivo: String): Resultado<RegistroPesaje> {
        val actual = obtener(pesajeId) ?: return Resultado.Fallo(ErrorApi.NoEncontrado("El pesaje ya no existe"))
        if (actual.id < 0) {
            return Resultado.Fallo(ErrorApi.Conflicto("Este pesaje aún no se envió al servidor: descártelo en lugar de anularlo"))
        }
        if (red.conectado.value) {
            when (val r = api.anularPesaje(pesajeId, motivo)) {
                is Resultado.Exito -> {
                    val anulado = actual.copy(estado = EstadoPesaje.Anulado(motivo), pendiente = false)
                    withContext(io) { db.guardarPesaje(anulado) }
                    return Resultado.Exito(RegistroPesaje.Enviado(anulado))
                }
                is Resultado.Fallo -> if (!r.error.esDeConectividad) return r
            }
        }
        withContext(io) { q.anularLocal(motivo, pesajeId) }
        return Resultado.Exito(RegistroPesaje.Pendiente(actual.copy(estado = EstadoPesaje.Anulado(motivo), pendiente = true)))
    }

    override suspend fun descartarPendiente(pesajeId: Long) {
        if (pesajeId < 0) withContext(io) { q.borrarPesaje(pesajeId) }
    }

    private suspend fun guardarCorreccionLocal(actual: Pesaje, datos: DatosPesaje, operacion: String): Pesaje =
        withContext(io) {
            val neto = netoProvisional(datos)
            q.corregirLocal(datos.material, datos.pesoBruto, datos.tara, neto, datos.observacion, datos.humedadEstimada,
                operacion, actual.id)
            actual.copy(material = datos.material, pesoBruto = datos.pesoBruto, tara = datos.tara, pesoNeto = neto,
                observacion = datos.observacion, humedadEstimada = datos.humedadEstimada, pendiente = true)
        }

    private fun netoProvisional(d: DatosPesaje): Double = round((d.pesoBruto - d.tara) * 1000) / 1000
}

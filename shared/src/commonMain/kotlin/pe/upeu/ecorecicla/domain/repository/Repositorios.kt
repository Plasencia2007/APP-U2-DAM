package pe.upeu.ecorecicla.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pe.upeu.ecorecicla.domain.model.DatosPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.RegistroPesaje
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Ruta
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.Subida

// Solo contratos: las implementaciones offline-first viven en data/repository.

interface AuthRepository {
    /** Sesion activa; pasa a null al cerrar sesion o cuando el servicio responde 401. */
    val sesion: StateFlow<Sesion?>

    /** true cuando la sesion termino porque el servicio respondio 401 (token vencido o alterado). */
    val sesionVencida: StateFlow<Boolean>

    suspend fun iniciarSesion(usuario: String, clave: String): Resultado<Sesion>

    /** Recupera la sesion guardada si el token sigue vigente; si vencio, la borra. */
    suspend fun recuperarSesion(): Sesion?

    /** Borra el token del almacenamiento seguro y deja la base local limpia. */
    suspend fun cerrarSesion()
}

interface RutaRepository {
    /** Ruta del dia leida de la base local, filtrada por el buscador. Flujo observable. */
    fun observarRuta(busqueda: String): Flow<Ruta?>

    fun observarPunto(puntoId: Long): Flow<PuntoAcopio?>

    /** Fecha y hora de la ultima sincronizacion correcta (ISO-8601) o null. */
    fun observarUltimaSincronizacion(): Flow<String?>

    suspend fun cerrarRuta(rutaId: Long): Resultado<Unit>
}

interface PesajeRepository {
    /** Pesajes de un punto, del mas reciente al mas antiguo, desde la base local. */
    fun observarPesajes(puntoId: Long): Flow<List<Pesaje>>

    suspend fun obtener(pesajeId: Long): Pesaje?

    suspend fun registrar(puntoId: Long, rutaId: Long, datos: DatosPesaje): Resultado<RegistroPesaje>

    suspend fun corregir(pesajeId: Long, datos: DatosPesaje): Resultado<RegistroPesaje>

    suspend fun anular(pesajeId: Long, motivo: String): Resultado<RegistroPesaje>

    /** SC-D: numero de pesajes pendientes de sincronizar, observado desde la base local. */
    fun observarPendientes(): Flow<Long>

    /** Descarta un pesaje que nunca llego al servicio (fila provisional). */
    suspend fun descartarPendiente(pesajeId: Long)
}

interface SincronizacionRepository {
    /** Paso 1: envia al servicio todas las filas marcadas como pendientes. */
    suspend fun subirPendientes(): Resultado<Subida>

    /** Paso 2: trae la ruta del dia y sus pesajes y los guarda en la base local. Devuelve cuantos recibio. */
    suspend fun bajarActualizaciones(): Resultado<Int>

    /** Registra la hora de la ultima sincronizacion correcta. */
    suspend fun marcarSincronizado()
}

/** Estado de la red del dispositivo (implementado en Android con ConnectivityManager). */
interface MonitorDeRed {
    val conectado: StateFlow<Boolean>
}

package pe.upeu.ecorecicla.data.remote

/**
 * Configuracion centralizada del cliente HTTP. La direccion base sale de BuildConfig en androidApp
 * y se inyecta con Koin: ninguna peticion escribe la URL completa.
 */
data class ApiConfig(
    val urlBase: String,
    /** true en depuracion; en produccion el registro se reduce a nada. */
    val registroDetallado: Boolean,
    val tiempoPeticionMs: Long = 15_000,
    val tiempoConexionMs: Long = 8_000
)

/** Rutas del servicio, relativas a la direccion base (que termina en /api/v1/). */
object Rutas {
    const val LOGIN = "auth/login"
    const val USUARIO_ACTUAL = "auth/me"
    const val RUTA_DEL_DIA = "rutas/hoy"
    fun pesajesDeRuta(rutaId: Long) = "rutas/$rutaId/pesajes"
    fun cierreDeRuta(rutaId: Long) = "rutas/$rutaId/cierre"
    fun pesajesDePunto(puntoId: Long) = "puntos/$puntoId/pesajes"
    fun pesaje(pesajeId: Long) = "pesajes/$pesajeId"
}

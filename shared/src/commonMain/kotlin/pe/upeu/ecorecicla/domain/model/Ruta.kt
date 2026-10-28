package pe.upeu.ecorecicla.domain.model

enum class EstadoRuta { ABIERTA, CERRADA }

/** Ruta del dia tal como la conoce la base local. */
data class Ruta(
    val id: Long,
    val fecha: String,
    val estado: EstadoRuta,
    val operario: String,
    val puntos: List<PuntoAcopio>
)

/** Resultado del paso de subida: cuantos pendientes llegaron y cuantos rechazo el servicio. */
data class Subida(val enviados: Int, val rechazados: Int)

/** Resumen que la sincronizacion muestra al terminar. */
data class ResumenSincronizacion(
    val enviados: Int,
    val recibidos: Int,
    val rechazados: Int
)

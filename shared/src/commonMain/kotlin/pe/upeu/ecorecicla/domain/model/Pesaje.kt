package pe.upeu.ecorecicla.domain.model

// domain/model/Pesaje.kt (seccion 4.5). Se agregan rutaId, observacion y errorSincronizacion
// al final y con valor por defecto, sin cambiar las propiedades entregadas.
data class Pesaje(
    val id: Long,                     // negativo mientras este pendiente de envio
    val puntoId: Long,
    val fechaHora: String,            // ISO-8601
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,             // lo calcula el servicio
    val operario: String,
    val estado: EstadoPesaje,
    val pendiente: Boolean,
    val rutaId: Long = 0,
    val observacion: String? = null,
    val errorSincronizacion: String? = null
)

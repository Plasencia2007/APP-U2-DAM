package pe.upeu.ecorecicla.domain.model

// domain/model/PuntoAcopio.kt (seccion 4.5). Se agrega "omitido" con valor por defecto.
data class PuntoAcopio(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,               // orden de visita en la ruta
    val atendido: Boolean,
    val omitido: Boolean = false
)

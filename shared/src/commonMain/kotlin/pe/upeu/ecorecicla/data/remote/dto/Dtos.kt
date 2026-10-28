package pe.upeu.ecorecicla.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTO propios de la capa de datos. El dominio no conoce ninguna anotacion de serializacion.
// Cuando el nombre Kotlin difiere del JSON se anota con @SerialName.

@Serializable
data class LoginPeticionDto(
    @SerialName("username") val usuario: String,
    @SerialName("password") val clave: String
)

@Serializable
data class LoginRespuestaDto(
    val token: String,
    val tipo: String = "Bearer",
    @SerialName("expiraEn") val expiraEnSegundos: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: List<String>
)

@Serializable
data class UsuarioActualDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: List<String>
)

/** Envoltorio de paginacion: por eso un listado no se deserializa como una lista. */
@Serializable
data class PaginaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

/** Formato de error comun del servicio. */
@Serializable
data class ErrorDto(
    val timestamp: String? = null,
    val status: Int = 0,
    val error: String? = null,
    @SerialName("message") val mensaje: String? = null,
    val path: String? = null,
    @SerialName("validationErrors") val erroresDeValidacion: Map<String, String>? = null
)

@Serializable
data class RutaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val operario: String,
    val supervisor: String? = null,
    val puntos: List<RutaPuntoDto> = emptyList()
)

@Serializable
data class RutaPuntoDto(
    @SerialName("id") val puntoId: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int,
    val atendido: Boolean = false,
    val omitido: Boolean = false
)

@Serializable
data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val rutaId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val observacion: String? = null,
    val operario: String,
    val estado: String,
    val motivoAnulacion: String? = null,
    val claveCliente: String? = null,
    val humedadEstimada: Double? = null
)

/** Peticion de registro. No existe campo de peso neto: el cliente nunca lo envia (RN-03). */
@Serializable
data class PesajePeticionDto(
    val rutaId: Long,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null,
    val claveCliente: String? = null,
    val humedadEstimada: Double? = null
)

@Serializable
data class CorreccionPesajeDto(
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String? = null,
    val humedadEstimada: Double? = null
)

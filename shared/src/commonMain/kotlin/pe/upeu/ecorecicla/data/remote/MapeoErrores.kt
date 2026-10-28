package pe.upeu.ecorecicla.data.remote

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pe.upeu.ecorecicla.data.remote.dto.ErrorDto
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Resultado

/**
 * Lugar UNICO donde cada respuesta HTTP se traduce al dominio:
 * 2xx -> se convierte el cuerpo; 4xx/5xx -> un caso de ErrorApi; fallo de red -> SinConexion / TiempoAgotado.
 */
suspend fun <T> llamar(
    peticion: suspend () -> HttpResponse,
    convertir: suspend (HttpResponse) -> T
): Resultado<T> {
    val respuesta = try {
        peticion()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        return Resultado.Fallo(ErrorApi.TiempoAgotado)
    } catch (e: ConnectTimeoutException) {
        return Resultado.Fallo(ErrorApi.TiempoAgotado)
    } catch (e: SocketTimeoutException) {
        return Resultado.Fallo(ErrorApi.TiempoAgotado)
    } catch (e: IOException) {
        return Resultado.Fallo(ErrorApi.SinConexion)
    }
    return traducir(respuesta, convertir)
}

suspend fun <T> traducir(respuesta: HttpResponse, convertir: suspend (HttpResponse) -> T): Resultado<T> {
    val codigo = respuesta.status.value
    if (codigo in 200..299) {
        return try {
            Resultado.Exito(convertir(respuesta))
        } catch (e: SerializationException) {
            Resultado.Fallo(ErrorApi.Desconocido("Respuesta del servidor con formato inesperado"))
        }
    }
    val error = leerError(respuesta)
    val mensaje = error?.mensaje
    return Resultado.Fallo(
        when (codigo) {
            400 -> ErrorApi.Validacion(mensaje ?: "Datos invalidos", error?.erroresDeValidacion.orEmpty())
            401 -> ErrorApi.NoAutorizado(mensaje ?: "Su sesion vencio. Inicie sesion nuevamente")
            403 -> ErrorApi.SinPrivilegio(mensaje ?: "No tiene permiso para realizar esta operacion")
            404 -> ErrorApi.NoEncontrado(mensaje ?: "El recurso ya no existe")
            409 -> ErrorApi.Conflicto(mensaje ?: "La operacion entra en conflicto con el estado del servidor")
            in 500..599 -> ErrorApi.Servidor(codigo)
            else -> ErrorApi.Desconocido("Respuesta inesperada del servidor ($codigo)")
        }
    )
}

/** Lee el formato de error comun (seccion 4.2); si el cuerpo no lo respeta, devuelve null. */
private suspend fun leerError(respuesta: HttpResponse): ErrorDto? = try {
    val texto = respuesta.bodyAsText()
    if (texto.isBlank()) null else jsonEco.decodeFromString(ErrorDto.serializer(), texto)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Atajo para respuestas con cuerpo JSON. */
suspend inline fun <reified T> HttpResponse.cuerpo(): T = body()

package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Json tolerante: ignora claves desconocidas que el servicio agregue en el futuro. */
val jsonEco = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

/**
 * Crea la UNICA instancia de HttpClient de la aplicacion (Koin la registra como single).
 *
 * @param motor OkHttp en Android, MockEngine en las pruebas.
 * @param tokenActual de donde se lee el token para la cabecera.
 * @param al401 que hacer cuando el servicio responde 401 (borrar token y volver al login).
 */
fun crearClienteHttp(
    motor: HttpClientEngine,
    config: ApiConfig,
    tokenActual: () -> String?,
    al401: () -> Unit
): HttpClient = HttpClient(motor) {
    // Los codigos de error se traducen en MapeoErrores, no lanzando excepciones.
    expectSuccess = false

    install(ContentNegotiation) { json(jsonEco) }

    install(HttpTimeout) {
        requestTimeoutMillis = config.tiempoPeticionMs
        connectTimeoutMillis = config.tiempoConexionMs
        socketTimeoutMillis = config.tiempoPeticionMs
    }

    install(Logging) {
        logger = Logger.SIMPLE
        // Registro reducido en produccion: solo en depuracion se ven las cabeceras de cada peticion.
        level = if (config.registroDetallado) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { it == HttpHeaders.Authorization }
    }

    defaultRequest {
        url(config.urlBase)
        contentType(ContentType.Application.Json)
    }

    // Punto UNICO donde se agrega "Authorization: Bearer <token>" a todas las peticiones.
    install(CabeceraJwt(tokenActual, al401))
}

/**
 * Plugin propio: agrega el token antes de enviar y, si llega un 401 en una ruta protegida,
 * avisa para cerrar la sesion. No hace falta tocar cada funcion de la API.
 */
private fun CabeceraJwt(tokenActual: () -> String?, al401: () -> Unit) =
    createClientPlugin("CabeceraJwt") {
        onRequest { request, _ ->
            val esLogin = request.url.encodedPathSegments.lastOrNull() == "login"
            val token = tokenActual()
            if (!esLogin && token != null) {
                request.headers[HttpHeaders.Authorization] = "Bearer $token"
            }
        }
        onResponse { response ->
            val esLogin = response.call.request.url.encodedPath.endsWith("/login")
            if (response.status == HttpStatusCode.Unauthorized && !esLogin) {
                al401()
            }
        }
    }

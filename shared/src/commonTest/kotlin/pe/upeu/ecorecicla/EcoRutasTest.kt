package pe.upeu.ecorecicla

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.remote.dto.PaginaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.data.remote.jsonEco
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SincronizacionRepositoryImpl
import pe.upeu.ecorecicla.domain.model.DatosPesaje
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.RegistroPesaje
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val JSON = headersOf(HttpHeaders.ContentType, "application/json")

class EcoRutasTest {

    // 1. Conversion de DTO: el envoltorio de paginacion y un pesaje anulado llegan bien al dominio.
    @Test
    fun conversionDeDtoAlDominio() {
        val cuerpo = """
            {"contenido":[{"id":7,"puntoId":3,"rutaId":2,"fechaHora":"2026-10-27T08:15:00","material":"Carton",
              "pesoBruto":120.5,"tara":8.0,"pesoNeto":112.5,"operario":"operario1","estado":"ANULADO",
              "motivoAnulacion":"Duplicado","campoNuevoQueNoExiste":true}],
             "pagina":0,"tamanio":20,"totalElementos":1,"totalPaginas":1,"ultima":true}
        """.trimIndent()
        val pagina = jsonEco.decodeFromString(PaginaDto.serializer(PesajeDto.serializer()), cuerpo)
        val pesaje = pagina.contenido.single().aDominio()

        assertEquals(1, pagina.totalElementos)
        assertTrue(pagina.ultima)
        assertEquals(7, pesaje.id)
        assertEquals(112.5, pesaje.pesoNeto)
        assertEquals(EstadoPesaje.Anulado("Duplicado"), pesaje.estado)
        assertFalse(pesaje.pendiente)
    }

    // 2. Traduccion de errores: 400 trae el detalle por campo, 409 el motivo y 401 borra la sesion.
    @Test
    fun traduccionDeErroresHttp() = runTest {
        val almacen = AlmacenEnMemoria()
        val gestor = GestorSesion(almacen, RelojFijo())
        gestor.guardar(sesionDePrueba())
        val (api, _) = apiSimulada(gestor) { req ->
            when (req.url.encodedPath) {
                "/api/v1/puntos/1/pesajes" -> respond(
                    """{"status":400,"message":"Datos invalidos","validationErrors":{"tara":"La tara debe ser menor"}}""",
                    HttpStatusCode.BadRequest, JSON
                )
                "/api/v1/rutas/2/cierre" -> respond(
                    """{"status":409,"message":"No se puede cerrar la ruta: faltan 3 puntos"}""",
                    HttpStatusCode.Conflict, JSON
                )
                else -> respond("""{"status":401,"message":"El token ha vencido"}""", HttpStatusCode.Unauthorized, JSON)
            }
        }

        val r400 = api.registrarPesaje(1, pe.upeu.ecorecicla.data.remote.dto.PesajePeticionDto(2, "Papel", 10.0, 12.0))
        val error400 = assertIs<ErrorApi.Validacion>(assertIs<Resultado.Fallo>(r400).error)
        assertEquals("La tara debe ser menor", error400.errores["tara"])

        val r409 = api.cerrarRuta(2)
        assertEquals("No se puede cerrar la ruta: faltan 3 puntos", assertIs<Resultado.Fallo>(r409).error.message)
        assertIs<ErrorApi.Conflicto>(r409.error)

        val r401 = api.rutaDelDia()
        assertIs<ErrorApi.NoAutorizado>(assertIs<Resultado.Fallo>(r401).error)
        assertNull(gestor.sesion.value, "un 401 debe cerrar la sesion")
        assertNull(almacen.leer("token"), "un 401 debe borrar el token del almacen seguro")
    }

    // 3. Registro sin conexion: queda en la base local, marcado como pendiente y con id provisional negativo.
    @Test
    fun registroSinConexionQuedaPendienteConIdNegativo() = runTest {
        val db = baseEnMemoria()
        val gestor = GestorSesion(AlmacenEnMemoria(), RelojFijo()).apply { guardar(sesionDePrueba()) }
        var peticiones = 0
        val (api, _) = apiSimulada(gestor) { peticiones++; respond("", HttpStatusCode.InternalServerError) }
        val repo = PesajeRepositoryImpl(db, api, RedSimulada(conectado = false), RelojFijo(), gestor, Dispatchers.Unconfined)

        val primero = repo.registrar(3, 2, DatosPesaje("Carton", 50.0, 5.0, null))
        val segundo = repo.registrar(3, 2, DatosPesaje("Papel", 20.0, 1.0, "fardo"))

        val p1 = assertIs<RegistroPesaje.Pendiente>(assertIs<Resultado.Exito<RegistroPesaje>>(primero).valor).pesaje
        val p2 = assertIs<RegistroPesaje.Pendiente>(assertIs<Resultado.Exito<RegistroPesaje>>(segundo).valor).pesaje
        assertEquals(-1, p1.id, "con la tabla vacia el primer id provisional es -1")
        assertEquals(-2, p2.id)
        assertEquals(0, peticiones, "sin red no se intenta llamar al servicio")

        val fila = db.ecoDbQueries.pesajePorId(-1).executeAsOne()
        assertTrue(fila.pendiente)
        assertEquals("CREAR", fila.operacion)
        assertEquals(2, db.ecoDbQueries.pendientes().executeAsList().size)
    }

    // 4. Orden de la sincronizacion: primero SUBE lo pendiente y despues BAJA; la fila provisional se
    //    reemplaza por la definitiva sin duplicarse ni perderse.
    @Test
    fun sincronizacionSubeAntesDeBajarSinDuplicar() = runTest {
        val db = baseEnMemoria()
        val gestor = GestorSesion(AlmacenEnMemoria(), RelojFijo()).apply { guardar(sesionDePrueba()) }
        val orden = mutableListOf<String>()
        val pesajeServidor = """{"id":16,"puntoId":3,"rutaId":2,"fechaHora":"2026-10-27T09:00:05","material":"Carton",
            "pesoBruto":50.0,"tara":5.0,"pesoNeto":45.0,"operario":"operario1","estado":"REGISTRADO","claveCliente":"k"}"""
        val (api, _) = apiSimulada(gestor) { req ->
            orden += "${req.method.value} ${req.url.encodedPath}"
            when {
                req.method == HttpMethod.Post && req.url.encodedPath == "/api/v1/puntos/3/pesajes" ->
                    respond(pesajeServidor, HttpStatusCode.Created, JSON)
                req.url.encodedPath == "/api/v1/rutas/hoy" -> respond(
                    """{"id":2,"fecha":"2026-10-27","estado":"ABIERTA","operario":"operario1","puntos":[
                       {"id":3,"razonSocial":"Imprenta","direccion":"Calle 1","zona":"Norte","materialHabitual":"Papel","secuencia":1}]}""",
                    HttpStatusCode.OK, JSON
                )
                req.url.encodedPath == "/api/v1/rutas/2/pesajes" -> respond(
                    """{"contenido":[$pesajeServidor],"pagina":0,"tamanio":50,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                    HttpStatusCode.OK, JSON
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        // Registro sin red -> pendiente con id -1.
        val repo = PesajeRepositoryImpl(db, api, RedSimulada(conectado = false), RelojFijo(), gestor, Dispatchers.Unconfined)
        repo.registrar(3, 2, DatosPesaje("Carton", 50.0, 5.0, null))

        val sincronizar = SincronizarUseCase(SincronizacionRepositoryImpl(db, api, RelojFijo(), Dispatchers.Unconfined))
        val resumen = assertIs<Resultado.Exito<pe.upeu.ecorecicla.domain.model.ResumenSincronizacion>>(sincronizar()).valor

        assertEquals("POST /api/v1/puntos/3/pesajes", orden.first(), "lo primero es subir el pendiente")
        assertTrue(orden.indexOf("GET /api/v1/rutas/hoy") > 0, "la bajada ocurre despues de la subida")
        assertEquals(1, resumen.enviados)

        val filas = db.ecoDbQueries.pesajesDePunto(3).executeAsList()
        assertEquals(1, filas.size, "la fila provisional se sustituye: ni duplicada ni perdida")
        assertEquals(16, filas.single().id)
        assertFalse(filas.single().pendiente)
        assertTrue(db.ecoDbQueries.pendientes().executeAsList().isEmpty())
    }

    // 5. Siguiente id provisional con la tabla vacia y con ids del servicio ya guardados.
    @Test
    fun idProvisionalNuncaColisionaConLosDelServicio() {
        val db = baseEnMemoria()
        assertEquals(-1, db.ecoDbQueries.siguienteIdProvisional().executeAsOne().MIN)
        db.ecoDbQueries.insertarPesaje(25, 1, 1, "2026-10-27T08:00:00", "Papel", 10.0, 1.0, 9.0, "op",
            null, "REGISTRADO", null, false, null, null, null, null)
        assertEquals(-1, db.ecoDbQueries.siguienteIdProvisional().executeAsOne().MIN)
        db.ecoDbQueries.insertarPesaje(-1, 1, 1, "2026-10-27T08:00:00", "Papel", 10.0, 1.0, 9.0, "op",
            null, "REGISTRADO", null, true, "CREAR", null, null, null)
        assertEquals(-2, db.ecoDbQueries.siguienteIdProvisional().executeAsOne().MIN)
    }
}

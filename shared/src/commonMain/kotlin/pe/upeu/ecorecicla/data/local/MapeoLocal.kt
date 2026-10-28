package pe.upeu.ecorecicla.data.local

import pe.upeu.ecorecicla.db.EcoDb
import pe.upeu.ecorecicla.db.PuntoPorId
import pe.upeu.ecorecicla.db.PuntosDeRuta
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.db.Pesaje as FilaPesaje

// Conversion entre filas de SQLDelight y el modelo de dominio.

const val ESTADO_REGISTRADO = "REGISTRADO"
const val ESTADO_ANULADO = "ANULADO"

object Operacion {
    const val CREAR = "CREAR"
    const val EDITAR = "EDITAR"
    const val ANULAR = "ANULAR"
}

object Metadatos {
    const val ULTIMA_SINCRONIZACION = "ultima_sincronizacion"
    const val USUARIO_DE_LA_BASE = "usuario"
}

fun FilaPesaje.aDominio(): Pesaje = Pesaje(
    id = id,
    puntoId = punto_id,
    fechaHora = fecha_hora,
    material = material,
    pesoBruto = peso_bruto,
    tara = tara,
    pesoNeto = peso_neto,
    operario = operario,
    estado = if (estado == ESTADO_ANULADO) EstadoPesaje.Anulado(motivo_anulacion.orEmpty()) else EstadoPesaje.Registrado,
    pendiente = pendiente,
    rutaId = ruta_id,
    observacion = observacion,
    errorSincronizacion = error_sync
)

fun PuntosDeRuta.aDominio(): PuntoAcopio = PuntoAcopio(
    id = id,
    razonSocial = razon_social,
    direccion = direccion,
    zona = zona,
    materialHabitual = material_habitual,
    secuencia = secuencia.toInt(),
    atendido = atendido,
    omitido = omitido
)

fun PuntoPorId.aDominio(): PuntoAcopio = PuntoAcopio(
    id = id,
    razonSocial = razon_social,
    direccion = direccion,
    zona = zona,
    materialHabitual = material_habitual,
    secuencia = secuencia.toInt(),
    atendido = atendido,
    omitido = omitido
)

/** Inserta o reemplaza una fila de pesaje a partir del dominio. */
fun EcoDb.guardarPesaje(p: Pesaje, operacion: String? = null, claveCliente: String? = null) {
    val anulado = p.estado as? EstadoPesaje.Anulado
    ecoDbQueries.insertarPesaje(
        id = p.id,
        punto_id = p.puntoId,
        ruta_id = p.rutaId,
        fecha_hora = p.fechaHora,
        material = p.material,
        peso_bruto = p.pesoBruto,
        tara = p.tara,
        peso_neto = p.pesoNeto,
        operario = p.operario,
        observacion = p.observacion,
        estado = if (anulado != null) ESTADO_ANULADO else ESTADO_REGISTRADO,
        motivo_anulacion = anulado?.motivo,
        pendiente = p.pendiente,
        operacion = operacion,
        clave_cliente = claveCliente,
        error_sync = p.errorSincronizacion
    )
}

/** Siguiente identificador provisional: siempre negativo, nunca colisiona con uno del servicio. */
fun EcoDb.siguienteIdProvisional(): Long =
    ecoDbQueries.siguienteIdProvisional().executeAsOneOrNull()?.MIN ?: -1L

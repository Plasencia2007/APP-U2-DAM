package pe.upeu.ecorecicla.data.remote.dto

import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.EstadoRuta
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.PuntoAcopio
import pe.upeu.ecorecicla.domain.model.Ruta
import pe.upeu.ecorecicla.domain.model.Sesion

// Funciones de conversion DTO -> dominio.

fun LoginRespuestaDto.aDominio(ahoraMillis: Long): Sesion = Sesion(
    token = token,
    usuario = usuario,
    rol = rol,
    privilegios = privilegios.toSet(),
    nombre = nombre,
    expiraEnMillis = ahoraMillis + expiraEnSegundos * 1000
)

fun RutaPuntoDto.aDominio(): PuntoAcopio = PuntoAcopio(
    id = puntoId,
    razonSocial = razonSocial,
    direccion = direccion,
    zona = zona,
    materialHabitual = materialHabitual,
    secuencia = secuencia,
    atendido = atendido,
    omitido = omitido
)

fun RutaDto.aDominio(): Ruta = Ruta(
    id = id,
    fecha = fecha,
    estado = if (estado == "CERRADA") EstadoRuta.CERRADA else EstadoRuta.ABIERTA,
    operario = operario,
    puntos = puntos.map { it.aDominio() }.sortedBy { it.secuencia }
)

fun PesajeDto.aDominio(): Pesaje = Pesaje(
    id = id,
    puntoId = puntoId,
    fechaHora = fechaHora,
    material = material,
    pesoBruto = pesoBruto,
    tara = tara,
    pesoNeto = pesoNeto,
    operario = operario,
    estado = if (estado == "ANULADO") EstadoPesaje.Anulado(motivoAnulacion.orEmpty()) else EstadoPesaje.Registrado,
    pendiente = false,
    rutaId = rutaId,
    observacion = observacion,
    humedadEstimada = humedadEstimada
)

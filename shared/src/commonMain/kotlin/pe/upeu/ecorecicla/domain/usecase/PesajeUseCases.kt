package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.ecorecicla.domain.model.DatosPesaje
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.RegistroPesaje
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.repository.PesajeRepository

class ObservarPesajesUseCase(private val pesajes: PesajeRepository) {
    operator fun invoke(puntoId: Long): Flow<List<Pesaje>> = pesajes.observarPesajes(puntoId)
}

/** SC-D: pendientes de sincronizar; viene de una consulta observable, no de un contador en memoria. */
class ObservarPendientesUseCase(private val pesajes: PesajeRepository) {
    operator fun invoke(): Flow<Long> = pesajes.observarPendientes()
}

class ObtenerPesajeUseCase(private val pesajes: PesajeRepository) {
    suspend operator fun invoke(pesajeId: Long): Pesaje? = pesajes.obtener(pesajeId)
}

/**
 * Validacion de formulario en el cliente (la misma que hara el servicio). Permite rechazar
 * datos imposibles aun sin conexion. El neto se valida pero NUNCA se envia.
 */
object ValidadorPesaje {
    fun validar(material: String, pesoBruto: String, tara: String): Pair<DatosPesaje?, Map<String, String>> {
        val errores = mutableMapOf<String, String>()
        val bruto = pesoBruto.replace(',', '.').trim().toDoubleOrNull()
        val t = tara.replace(',', '.').trim().toDoubleOrNull()
        if (material.isBlank()) errores["material"] = "El material es obligatorio"
        if (bruto == null) errores["pesoBruto"] = "Ingrese el peso bruto en kg"
        else if (bruto <= 0) errores["pesoBruto"] = "El peso bruto debe ser mayor que cero"
        if (t == null) errores["tara"] = "Ingrese la tara en kg"
        else if (t < 0) errores["tara"] = "La tara no puede ser negativa"
        if (bruto != null && t != null && bruto > 0 && t >= 0 && bruto - t <= 0) {
            errores["tara"] = "La tara debe ser menor que el peso bruto"
        }
        if (errores.isNotEmpty() || bruto == null || t == null) return null to errores
        return DatosPesaje(material.trim(), bruto, t, null) to emptyMap()
    }
}

class RegistrarPesajeUseCase(private val pesajes: PesajeRepository) {
    suspend operator fun invoke(
        puntoId: Long, rutaId: Long, material: String, pesoBruto: String, tara: String, observacion: String
    ): Resultado<RegistroPesaje> {
        val (datos, errores) = ValidadorPesaje.validar(material, pesoBruto, tara)
        if (datos == null) return Resultado.Fallo(ErrorApi.Validacion("Revise los datos del formulario", errores))
        return pesajes.registrar(puntoId, rutaId, datos.copy(observacion = observacion.ifBlank { null }))
    }
}

class CorregirPesajeUseCase(private val pesajes: PesajeRepository) {
    suspend operator fun invoke(
        pesajeId: Long, material: String, pesoBruto: String, tara: String, observacion: String
    ): Resultado<RegistroPesaje> {
        val (datos, errores) = ValidadorPesaje.validar(material, pesoBruto, tara)
        if (datos == null) return Resultado.Fallo(ErrorApi.Validacion("Revise los datos del formulario", errores))
        return pesajes.corregir(pesajeId, datos.copy(observacion = observacion.ifBlank { null }))
    }
}

class AnularPesajeUseCase(private val pesajes: PesajeRepository) {
    suspend operator fun invoke(pesajeId: Long, motivo: String): Resultado<RegistroPesaje> {
        if (motivo.isBlank()) {
            return Resultado.Fallo(ErrorApi.Validacion("Indique el motivo", mapOf("motivo" to "El motivo es obligatorio")))
        }
        return pesajes.anular(pesajeId, motivo.trim())
    }
}

class DescartarPendienteUseCase(private val pesajes: PesajeRepository) {
    suspend operator fun invoke(pesajeId: Long) = pesajes.descartarPendiente(pesajeId)
}

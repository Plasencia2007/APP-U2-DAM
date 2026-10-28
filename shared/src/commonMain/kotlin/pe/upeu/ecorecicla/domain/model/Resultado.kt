package pe.upeu.ecorecicla.domain.model

/** Resultado de una operacion: exito con valor o fallo con un error tipado. */
sealed interface Resultado<out T> {
    data class Exito<T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorApi) : Resultado<Nothing>
}

inline fun <T, R> Resultado<T>.map(transformar: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Exito -> Resultado.Exito(transformar(valor))
    is Resultado.Fallo -> this
}

/** Datos que el formulario envia para registrar o corregir un pesaje. El neto NO se incluye. */
data class DatosPesaje(
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val observacion: String?,
    val humedadEstimada: Double? = null
)

/** Como termino el registro de un pesaje. */
sealed interface RegistroPesaje {
    /** El servicio lo recibio: la fila tiene su id definitivo. */
    data class Enviado(val pesaje: Pesaje) : RegistroPesaje

    /** No hubo conexion: quedo guardado localmente como pendiente, con id provisional negativo. */
    data class Pendiente(val pesaje: Pesaje) : RegistroPesaje
}

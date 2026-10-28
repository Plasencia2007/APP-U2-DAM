package pe.upeu.ecorecicla.domain.model

// domain/model/EstadoPesaje.kt (seccion 4.5)
sealed interface EstadoPesaje {
    data object Registrado : EstadoPesaje
    data class Anulado(val motivo: String) : EstadoPesaje
}

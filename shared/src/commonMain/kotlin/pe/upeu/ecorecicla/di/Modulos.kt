package pe.upeu.ecorecicla.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.local.crearBase
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.crearClienteHttp
import pe.upeu.ecorecicla.data.repository.AuthRepositoryImpl
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.data.repository.RutaRepositoryImpl
import pe.upeu.ecorecicla.data.repository.SincronizacionRepositoryImpl
import pe.upeu.ecorecicla.domain.repository.AuthRepository
import pe.upeu.ecorecicla.domain.repository.PesajeRepository
import pe.upeu.ecorecicla.domain.repository.RutaRepository
import pe.upeu.ecorecicla.domain.repository.SincronizacionRepository
import pe.upeu.ecorecicla.domain.usecase.AnularPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.CerrarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.CerrarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.CorregirPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.DescartarPendienteUseCase
import pe.upeu.ecorecicla.domain.usecase.IniciarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPendientesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPesajesUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarPuntoUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.upeu.ecorecicla.domain.usecase.ObtenerPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.PermisosUseCase
import pe.upeu.ecorecicla.domain.usecase.RecuperarSesionUseCase
import pe.upeu.ecorecicla.domain.usecase.RegistrarPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.SincronizarUseCase
import pe.upeu.ecorecicla.presentation.detalle.DetallePuntoViewModel
import pe.upeu.ecorecicla.presentation.detalle.FormPesajeViewModel
import pe.upeu.ecorecicla.presentation.listado.RutaViewModel
import pe.upeu.ecorecicla.presentation.navigation.Navegador
import pe.upeu.ecorecicla.presentation.perfil.PerfilViewModel
import pe.upeu.ecorecicla.presentation.sesion.LoginViewModel
import pe.upeu.ecorecicla.presentation.sesion.SesionViewModel

val IO = named("io")

/**
 * redModule: la UNICA instancia de HttpClient. El motor (OkHttp) y ApiConfig los aporta platformModule,
 * que vive en androidApp (PlatformModule.android.kt); las pruebas aportan MockEngine.
 */
val redModule = module {
    single {
        val gestor = get<GestorSesion>()
        crearClienteHttp(
            motor = get(),
            config = get(),
            tokenActual = { gestor.tokenActual() },
            al401 = { gestor.invalidarPor401() }
        )
    }
    single { EcoApi(get()) }
}

val dataModule = module {
    single<CoroutineDispatcher>(IO) { Dispatchers.IO }
    single { get<BaseLocal>().crearBase() }
    single { GestorSesion(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get(IO)) }
    single<RutaRepository> { RutaRepositoryImpl(get(), get(), get(IO)) }
    single<PesajeRepository> { PesajeRepositoryImpl(get(), get(), get(), get(), get(), get(IO)) }
    single<SincronizacionRepository> { SincronizacionRepositoryImpl(get(), get(), get(), get(IO)) }
}

val domainModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { RecuperarSesionUseCase(get()) }
    factory { ObservarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObservarRutaUseCase(get()) }
    factory { ObservarPuntoUseCase(get()) }
    factory { ObservarUltimaSincronizacionUseCase(get()) }
    factory { CerrarRutaUseCase(get()) }
    factory { ObservarPesajesUseCase(get()) }
    factory { ObtenerPesajeUseCase(get()) }
    factory { ObservarPendientesUseCase(get()) }
    factory { RegistrarPesajeUseCase(get()) }
    factory { CorregirPesajeUseCase(get()) }
    factory { AnularPesajeUseCase(get()) }
    factory { DescartarPendienteUseCase(get()) }
    factory { PermisosUseCase() }
    // single: su Mutex evita dos sincronizaciones simultaneas.
    single { SincronizarUseCase(get()) }
}

val presentationModule = module {
    single { Navegador() }
    viewModelOf(::SesionViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RutaViewModel)
    viewModelOf(::DetallePuntoViewModel)
    viewModelOf(::FormPesajeViewModel)
    viewModelOf(::PerfilViewModel)
}

/** Todos los modulos; platformModule llega desde androidApp. */
fun modulosEcoRutas(platformModule: Module): List<Module> =
    listOf(platformModule, redModule, dataModule, domainModule, presentationModule)

# EcoRutas — Aplicación Android (Kotlin Multiplatform)

Aplicación del **Examen Parcial U2 – Versión B (caso EcoRecicla)** que el operario usa durante el recorrido.
Consume el servicio propio **EcoAPI** ([BACKEND-U2-DAM](https://github.com/Plasencia2007/BACKEND-U2-DAM)), funciona **sin conexión** (offline-first) y respeta los privilegios del usuario.

| Tecnología | Versión |
|---|---|
| Kotlin Multiplatform | 2.4.10 (objetivo: **Android**) |
| Compose Multiplatform / Material 3 | 1.11.1 / 1.11.0-alpha07 |
| Ktor Client (OkHttp en Android, MockEngine en pruebas) | 3.6.0 |
| SQLDelight | 2.4.1 |
| Koin | 4.0.2 |
| AndroidX Security Crypto (EncryptedSharedPreferences + Keystore) | 1.1.0 |
| Android Gradle Plugin / Gradle | 9.1.1 / 9.5.1 |

---

## 1. Ejecución

1. Levanta el backend (ver su README): `./gradlew bootRun` en `BACKEND-U2-DAM`.
2. Abre este proyecto en Android Studio (o usa la terminal) con JDK 21.
3. Ejecuta en un emulador Android:

```bash
./gradlew :androidApp:installDebug
```

La dirección base es `http://10.0.2.2:8080/api/v1/` (la PC vista desde el emulador). Se define **una sola vez** en `androidApp/build.gradle.kts` (`BuildConfig.URL_BASE`) y llega al cliente mediante `ApiConfig`. Para un teléfono físico, cambia esa constante por la IP de la PC y agrégala a `res/xml/network_security_config.xml`.

Usuarios de prueba (contraseña `Eco2026*`): `operario1`, `supervisor1`, `admin`.

### Pruebas (commonTest)

```bash
./gradlew :shared:testAndroidHostTest
```

Se ejecutan en la JVM, **sin emulador**: la red se sustituye por `MockEngine` de Ktor y la base por SQLite **en memoria** (`JdbcSqliteDriver.IN_MEMORY`). Reporte: `shared/build/reports/tests/`.

| # | Prueba | Garantiza |
|---|---|---|
| 1 | `conversionDeDtoAlDominio` | El envoltorio de paginación y un pesaje anulado se convierten bien al dominio; las claves desconocidas se ignoran. |
| 2 | `traduccionDeErroresHttp` | 400 → `Validacion` con detalle por campo; 409 → `Conflicto` con el motivo; 401 → `NoAutorizado` **y borra el token**. |
| 3 | `registroSinConexionQuedaPendienteConIdNegativo` | Sin red no se llama al servicio; la fila queda `pendiente = 1`, operación `CREAR` e id `-1`, `-2`… |
| 4 | `sincronizacionSubeAntesDeBajarSinDuplicar` | La primera petición es el `POST` del pendiente y después el `GET` de la ruta; la fila `-1` se sustituye por la `16` sin duplicarse ni perderse. |
| 5 | `idProvisionalNuncaColisionaConLosDelServicio` | La consulta del siguiente id provisional devuelve `-1` con la tabla vacía y nunca un valor del rango del servicio. |

---

## 2. Estructura de paquetes

```
shared/src/commonMain/kotlin/pe/upeu/ecorecicla/
├── domain/
│   ├── model/       PuntoAcopio, EstadoPesaje, Pesaje, Sesion (seccion 4.5), Ruta,
│   │                ErrorApi.kt (jerarquia sellada), Resultado
│   ├── repository/  AuthRepository, RutaRepository, PesajeRepository,
│   │                SincronizacionRepository, MonitorDeRed, Reloj (solo contratos)
│   └── usecase/     SincronizarUseCase.kt, sesion, ruta, pesajes, PermisosUseCase
├── data/
│   ├── remote/      ClienteHttp.kt, ApiConfig.kt, MapeoErrores.kt, EcoApi.kt
│   │   └── dto/     Dtos.kt (con @SerialName) y Conversiones.kt (DTO -> dominio)
│   ├── local/       AlmacenSeguro.kt (interfaz), BaseLocal.kt, GestorSesion.kt, MapeoLocal.kt
│   └── repository/  AuthRepositoryImpl, RutaRepositoryImpl, PesajeRepositoryImpl,
│                    SincronizacionRepositoryImpl (offline-first)
├── presentation/    sesion/, listado/, detalle/, perfil/, navigation/, theme/, componentes/
└── di/              Modulos.kt: redModule, dataModule, domainModule, presentationModule
shared/src/commonMain/sqldelight/pe/upeu/ecorecicla/db/EcoDb.sq   esquema local

androidApp/src/main/kotlin/pe/upeu/ecorecicla/
├── AlmacenSeguroAndroid.kt     preferencias cifradas + almacen de claves
├── BaseLocalAndroid.kt         driver de SQLDelight para Android
├── MonitorDeRedAndroid.kt      ConnectivityManager -> sincronizacion automatica
├── PlatformModule.android.kt   platformModule: motor OkHttp, ApiConfig, almacen, driver, red, reloj
├── EcoRutasApplication.kt      startKoin
└── MainActivity.kt
```

---

## 3. Contrato del API consumido

| Operación | Verbo y ruta | Uso en la app |
|---|---|---|
| Inicio de sesión | `POST /auth/login` | Pantalla de inicio de sesión |
| Ruta del día | `GET /rutas/hoy` | Sincronización (bajada) |
| Pesajes de la ruta | `GET /rutas/{id}/pesajes?pagina&tamanio` | Sincronización; se recorren todas las páginas del envoltorio |
| Registrar pesaje | `POST /puntos/{id}/pesajes` (201) | Formulario y subida de pendientes (con `claveCliente`) |
| Corregir pesaje | `PUT /pesajes/{id}` | Formulario de corrección y pendientes `EDITAR` |
| Anular pesaje | `DELETE /pesajes/{id}?motivo=` (204) | Diálogo "Anular" y pendientes `ANULAR` |
| Cerrar ruta | `POST /rutas/{id}/cierre` | Botón "Cerrar ruta del día" (supervisor) |

Todos los listados llegan en el envoltorio `{contenido, pagina, tamanio, totalElementos, totalPaginas, ultima}` (`PaginaDto<T>`); por eso un listado nunca se deserializa como lista. Los errores llegan en el formato común `{timestamp, status, error, message, path, validationErrors}` (`ErrorDto`).

---

## 4. Roles y privilegios en la interfaz

La app recibe los privilegios en el login y los guarda con la sesión. `PermisosUseCase` decide qué acciones **ofrecer**, siempre por privilegio y nunca por nombre de rol, y lo hace fuera de los composables. Los ViewModel exponen flags (`puedeRegistrar`, `puedeAnular`, `puedeCorregir`, `puedeCerrarRuta`) y la pantalla solo los pinta.

| Acción | Privilegio | OPERARIO | SUPERVISOR | ADMIN |
|---|---|:-:|:-:|:-:|
| Ver ruta y puntos | `PUNTO_LEER` | ✔ | ✔ | ✔ |
| Registrar pesaje | `PESAJE_CREAR` | ✔ | ✔ | — |
| Corregir | `PESAJE_EDITAR` (+ `PESAJE_EDITAR_TODOS`) | propios del día | todos | — |
| Anular | `PESAJE_ANULAR` | — | ✔ | — |
| Cerrar ruta | `RUTA_CERRAR` | — | ✔ | — |

> **SC-B (AUDITOR):** la app no necesitó cambios de código: el auditor recibe solo `PUNTO_LEER` y `PESAJE_LEER`, así que `PermisosUseCase` no le ofrece registrar, corregir, anular ni cerrar. Lo garantiza la prueba `PermisosAuditorTest`.

> Ocultar el botón es **experiencia de usuario**, no seguridad: el backend vuelve a verificar el privilegio y responde **403** aunque se invoque directamente.

---

## 5. Decisiones de arquitectura

### Capas
`presentation → domain ← data`. El dominio no depende de Ktor, SQLDelight ni de anotaciones de serialización. Los ViewModel usan `StateFlow<UiState>` y llaman a casos de uso; los repositorios del dominio son interfaces implementadas en `data/repository`. Todo se inyecta con **Koin**.

### Cliente HTTP único (`ClienteHttp.kt`)
- **Una sola instancia** de `HttpClient` (`single` en `redModule`) con `ContentNegotiation` (Json con `ignoreUnknownKeys`), `HttpTimeout`, `defaultRequest { url(ApiConfig.urlBase) }` y `Logging` (`HEADERS` en depuración, `NONE` en producción, ocultando `Authorization`).
- **Cabecera centralizada:** el plugin propio `CabeceraJwt` agrega `Authorization: Bearer <token>` en `onRequest` para todas las peticiones. Si se hiciera función por función, bastaría olvidarla en una para dejar un 401 difícil de rastrear, y cambiar el esquema obligaría a tocar todas.
- El mismo plugin, en `onResponse`, detecta un **401** y llama a `GestorSesion.invalidarPor401()`: borra el token y la sesión pasa a `null`, con lo que la app vuelve al login con el aviso "Su sesión venció".

### Errores tipados (`ErrorApi.kt` + `MapeoErrores.kt`)
`MapeoErrores.llamar()` es el único lugar donde una respuesta se traduce al dominio:

| Respuesta | ErrorApi | Conducta en la interfaz |
|---|---|---|
| 2xx | — | Se convierte el DTO al dominio y se guarda la fila definitiva (204: solo se comprueba el código) |
| 400 | `Validacion(errores)` | Cada mensaje bajo su campo del formulario |
| 401 | `NoAutorizado` | Se borra el token, se limpia la sesión y se vuelve al login |
| 403 | `SinPrivilegio` | "Operación no autorizada"; la sesión se mantiene |
| 404 | `NoEncontrado` | Se informa y se refresca el listado (sincroniza); sin reintento |
| 409 | `Conflicto` | Se muestra el motivo del servidor (p. ej. RN-04 "faltan 2 puntos") |
| 5xx | `Servidor` | Mensaje breve y botón **Reintentar** |
| IOException | `SinConexion` | Se guarda como pendiente / se trabaja con la base local |
| Timeout | `TiempoAgotado` | Igual que sin conexión, con reintento |

### Sesión y token
- `AlmacenSeguroAndroid` usa **EncryptedSharedPreferences** con una `MasterKey` AES-256-GCM custodiada por el **Android Keystore**: el archivo `sesion_ecorutas.xml` no contiene el token en claro. En preferencias sin cifrar, cualquiera con acceso al almacenamiento (respaldo, root, depuración) leería un token válido por una hora.
- Al reabrir la app, `SesionViewModel` llama a `RecuperarSesionUseCase`: si el vencimiento guardado sigue en el futuro entra directo; si venció, borra el token y muestra el login.
- **Cierre de sesión** (perfil): si hay red intenta subir lo pendiente, después borra el token del almacén seguro y **vacía la base local**, para dejar el equipo limpio al siguiente turno. Si inicia sesión otro usuario en un equipo con datos de otra persona, la base también se limpia.

### Offline-first con SQLDelight
- La pantalla principal **observa la base local** (`rutaActual().asFlow()` + `puntosDeRuta().asFlow()`): con modo avión sigue mostrando la ruta. La red solo alimenta esa base.
- `atendido` se calcula en la propia consulta con los pesajes locales vigentes, así un pesaje registrado sin señal marca el punto como pesado al instante.
- **Esquema derivado del dominio** (`EcoDb.sq`): `ruta`, `punto_ruta`, `pesaje` (con `pendiente`, `operacion`, `clave_cliente` y `error_sync`) y `metadato`.
- **Identificadores provisionales negativos:**
  ```sql
  SELECT MIN(-1, COALESCE(MIN(id), 0) - 1) FROM pesaje;
  ```
  Con la tabla vacía `MIN(id)` es `NULL` → `COALESCE` da `0` → devuelve `-1`. El servicio solo emite ids positivos (`IDENTITY`), así que nunca hay colisión, y un id negativo identifica de un vistazo la fila sin enviar.

### Sincronización (`SincronizarUseCase`)
1. **Sube** cada pendiente en orden. Si es `CREAR` y el servicio responde 201, en **una misma transacción** se borra la fila `-1` y se inserta la definitiva. Los rechazos de negocio (400/403/404/409) dejan la fila pendiente con el motivo visible; la falta de red, el 401 o el 5xx detienen la sincronización sin perder nada.
2. **Baja** la ruta del día y todos sus pesajes (todas las páginas) y reemplaza la copia local, **sin pisar** filas con cambios pendientes.
3. Guarda la hora de la última sincronización (visible en la ruta).

**¿Por qué subir antes de bajar?** Si se bajara primero, la copia local se reemplazaría con datos del servidor que aún no conocen lo registrado sin señal: el punto volvería a "por pesar" y una corrección hecha offline sería pisada por el valor viejo.

Se dispara **manualmente** (botón ⟳) y **automáticamente** cuando `MonitorDeRedAndroid` detecta que volvió la conexión, al iniciar sesión. Un `Mutex` impide dos sincronizaciones simultáneas, y la `claveCliente` (UUID) hace que un reenvío nunca duplique el pesaje en el servidor.

---

## 6. Solicitudes de cambio (Parte II)

Cada solicitud vive en su propia rama, creada desde `develop`, con tres commits (compila, funciona, verificada) y su PR abierto hacia `develop` para revisión.

| Rama | Solicitud | Dónde está el cambio | Prueba |
|---|---|---|---|
| `sc-a-plasencia` | Filtro «Solo pendientes de pesar» | Consulta `puntosDeRuta(:busqueda, :soloPendientes)` → `ObservarRutaUseCase` → `RutaViewModel` | `FiltroPendientesTest` |
| `sc-b-plasencia` | Rol AUDITOR | Sin cambios de interfaz: `PermisosUseCase` decide por privilegio | `PermisosAuditorTest` |
| `sc-c-plasencia` | Humedad estimada | DTO, dominio, columna `humedad_estimada` + migración `1.sqm`, formulario y detalle | `HumedadEstimadaTest` |
| `sc-d-plasencia` | Distintivo de pendientes | Consulta observable `contarPendientes` → `ObservarPendientesUseCase` → `Badge` | `PendientesPorEnviarTest` |

## 7. Flujo de Git

`main` (solo fusiones desde `develop`) · `develop` (integración) · `feature/*` · `fix/*` · `docs/*` · en el examen `sc-<letra>-<apellido>`. Fusiones con `--no-ff`; la versión evaluada se etiqueta `v1.0-unidad2`.

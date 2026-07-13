# Calidad Leche Service

Backend Spring Boot para el modulo de calidad de leche del MVP de una aplicacion para queserias.

El servicio permite registrar proveedores, cargar muestras de leche, evaluar parametros de calidad y consultar historicos/resumenes analiticos.

## Stack

- Java 21
- Spring Boot 3.5
- Spring WebFlux
- PostgreSQL con R2DBC
- Flyway para migraciones
- MongoDB Reactive para analiticas
- JUnit 5, Mockito y Reactor Test

## Arquitectura

El proyecto sigue una organizacion cercana a Clean Architecture:

```text
domain
  model      Reglas y modelos del negocio
  repo       Contratos de persistencia del dominio
  service    Servicios de dominio

application
  port       Puertos usados por casos de uso
  usecase    Casos de uso de la aplicacion

infrastructure
  config      Configuracion Spring
  persistence Implementaciones R2DBC y Mongo
  util        Utilidades tecnicas

interfaces
  web         Controllers REST y manejo de errores
```

El flujo principal para registrar una muestra es:

```text
MuestraController
  -> RegistrarMuestraConEvaluacionUseCase
  -> MuestraRepository/PostgreSQL
  -> EvaluacionCalidadService
  -> AnaliticaRepository/MongoDB
```

## Requisitos Locales

- JDK 21
- PostgreSQL local con base de datos `queseria`
- MongoDB local en `mongodb://localhost:27017`

Configuracion por defecto en desarrollo:

```properties
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/queseria
spring.r2dbc.username=postgres
spring.r2dbc.password=${DB_PASSWORD:root}
spring.data.mongodb.uri=${MONGO_URI:mongodb://localhost:27017}
```

## Ejecutar En Local

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en:

```text
http://localhost:8080
```

Health check:

```text
GET http://localhost:8080/actuator/health
```

## Correr Tests

```powershell
.\mvnw.cmd test
```

La suite cubre dominio, casos de uso, evaluacion de calidad y endpoints de proveedores con `WebTestClient`.

## Variables De Produccion

Perfil productivo:

```properties
spring.profiles.active=prod
```

Variables esperadas:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_R2DBC_URL
SPRING_R2DBC_USERNAME
SPRING_R2DBC_PASSWORD
SPRING_FLYWAY_URL
SPRING_FLYWAY_USER
SPRING_FLYWAY_PASSWORD
MONGO_URI
JWT_SECRET
JWT_ISSUER
JWT_EXPIRATION_MINUTES
PORT
```

`JWT_SECRET` es obligatorio con el perfil `prod` y debe tener al menos 32 caracteres.
`JWT_ISSUER` y `JWT_EXPIRATION_MINUTES` usan por defecto `calidad-leche-service` y `60`, respectivamente.

Formatos esperados:

```text
SPRING_R2DBC_URL=r2dbc:postgresql://host:port/database
SPRING_FLYWAY_URL=jdbc:postgresql://host:port/database
MONGO_URI=mongodb+srv://usuario:password@cluster.mongodb.net/database
```

En Render, el Health Check debe apuntar a:

```text
/actuator/health
```

Por defecto solo se expone `health` de Actuator. En desarrollo se expone Actuator completo desde `application-dev.properties`.

## Endpoints

### Proveedores

Crear proveedor:

```http
POST /api/v1/proveedores
Content-Type: application/json

{
  "nombre": "Proveedor Demo",
  "tipoIdentificacion": "NIT",
  "identificacion": "900123456"
}
```

Buscar por ID:

```http
GET /api/v1/proveedores/{id}
```

Actualizar proveedor:

```http
PUT /api/v1/proveedores/{id}
Content-Type: application/json

{
  "nombre": "Proveedor Actualizado",
  "tipoIdentificacion": "NIT",
  "identificacion": "900123456"
}
```

Este endpoint actualiza los datos del proveedor, pero no cambia su estado `activo`.

Activar proveedor:

```http
PATCH /api/v1/proveedores/{id}/activar
```

Desactivar proveedor:

```http
PATCH /api/v1/proveedores/{id}/desactivar
```

Buscar por identificacion:

```http
GET /api/v1/proveedores?identificacion=900123456
```

Listar proveedores:

```http
GET /api/v1/proveedores?q=&activo=true&limit=20&offset=0
```

### Muestras

Registrar muestra:

```http
POST /api/v1/muestras
Content-Type: application/json

{
  "proveedorId": 1,
  "fechaMuestra": "2026-01-12T08:00:00-05:00",
  "volumenLitros": 120.5,
  "precioLitro": 1800,
  "observaciones": "Muestra inicial",
  "composicion": {
    "grasa": 4.0,
    "proteina": 3.2,
    "lactosa": 4.7,
    "solidosTotales": 13.0
  },
  "fisicoQuimico": {
    "densidad": 1.032,
    "acidezDornic": 15,
    "temperaturaC": 18
  },
  "higiene": {
    "ufcBacterias": 1000,
    "ccSomaticas": 200000
  },
  "sng": null,
  "aguaPct": 0
}
```

Historico paginado por proveedor:

```http
GET /api/v1/muestras?proveedorId=1&desde=2026-01-01T00:00:00-05:00&hasta=2026-01-31T23:59:59-05:00&limit=20&offset=0
```

### Analiticas

Ultima analitica por muestra:

```http
GET /api/v1/analiticas/muestra/{sampleId}
```

Resumen por proveedor:

```http
GET /api/v1/analiticas/proveedor/{proveedorId}/resumen?desde=2026-01-01T00:00:00Z&hasta=2026-01-31T23:59:59Z
```

## Notas

- PostgreSQL almacena proveedores y muestras.
- MongoDB almacena documentos analiticos derivados de cada muestra.
- Flyway ejecuta migraciones desde `classpath:db/migration`.
- El frontend consume esta API desde Angular.

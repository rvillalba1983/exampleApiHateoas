# exampleApiHateoas

[![CI](https://github.com/rvillalba1983/exampleApiHateoas/actions/workflows/ci.yml/badge.svg)](https://github.com/rvillalba1983/exampleApiHateoas/actions/workflows/ci.yml)

API REST **HATEOAS** generada con Spring Data REST y protegida con **JWT**: login con usuario y contraseña, token de acceso, token de refresco y permisos por rol.

**Stack:** Java 25 · Spring Boot 4.1 · Spring Data REST · Spring Security (OAuth2 resource server) · JPA + H2

## Qué incluye

- **CRUD sin controladores.** `ExampleRepository` extiende `JpaRepository` y Spring Data REST lo publica en `/api/examples`, con paginación, búsqueda y enlaces HAL en cada respuesta.
- **Validación.** Las anotaciones `@NotBlank` y `@Size` de la entidad se aplican antes de crear o modificar; si fallan, se devuelve un 400.
- **JWT con Spring Security estándar.** La aplicación firma sus propios tokens (HMAC-SHA256) con `NimbusJwtEncoder` y los valida con el soporte de *resource server*. No hay filtros ni parsers hechos a mano.
- **Dos tipos de token:**
  - El **de acceso** dura 15 minutos y se envía en cada petición.
  - El **de refresco** dura 60 minutos y solo sirve para pedir un par nuevo.

  Cada tipo se valida con su propio decoder, así que no se puede usar uno en lugar del otro.
- **Roles.** Cualquier usuario autenticado puede leer, crear y modificar; solo `ADMIN` puede borrar.

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | `{"username","password"}` → tokens |
| `POST` | `/api/auth/refresh` | Público | `{"refreshToken"}` → nuevo par de tokens |
| `GET` | `/api/me` | Token | Usuario y roles del token |
| `GET` | `/api` | Token | Raíz HAL con los recursos disponibles |
| `GET` | `/api/examples` | Token | Listado paginado (`?page=0&size=20&sort=name`) |
| `GET` | `/api/examples/search/by-name?name=...` | Token | Búsqueda por nombre |
| `POST` | `/api/examples` | Token | Crear |
| `GET` / `PUT` / `PATCH` | `/api/examples/{id}` | Token | Consultar / modificar |
| `DELETE` | `/api/examples/{id}` | `ADMIN` | Borrar |
| `GET` | `/api/explorer` | Token | Navegador web de la API (HAL Explorer) |

Usuarios de ejemplo, definidos en memoria en `SecurityConfiguration`:

| Usuario | Contraseña | Roles |
|---|---|---|
| `user` | `password` | `USER` |
| `admin` | `admin` | `USER`, `ADMIN` |

## Probar

```bash
mvn spring-boot:run

TOKEN=$(curl -s -H 'Content-Type: application/json' -d '{"username":"user","password":"password"}' \
        localhost:8081/api/auth/login | jq -r .accessToken)

curl -s -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"name":"mi ejemplo"}' localhost:8081/api/examples
```

```json
{
  "id": 1,
  "name": "mi ejemplo",
  "_links": {
    "self": { "href": "http://localhost:8081/api/examples/1" },
    "example": { "href": "http://localhost:8081/api/examples/1" }
  }
}
```

## Configuración

```properties
app.jwt.issuer=example-api-hateoas
app.jwt.secret=${APP_JWT_SECRET:...}   # Base64, mínimo 32 bytes
app.jwt.access-token-ttl=15m
app.jwt.refresh-token-ttl=60m
```

La clave incluida es **solo para desarrollo**; en cualquier otro entorno hay que definir `APP_JWT_SECRET`. Para generar una: `openssl rand -base64 32`.

La base de datos es H2 en memoria. Para usar MariaDB o PostgreSQL, basta con añadir el driver y configurar `spring.datasource.*`.

## Tests

`ExampleApiTest` prueba la API completa con `MockMvcTester`:
- login correcto y fallido;
- 401 sin token;
- el CRUD entero con enlaces HAL, listado y búsqueda;
- 403 al borrar como `USER`;
- validación;
- refresco de token;
- que un token de refresco no abre la API (ni al revés);
- tokens manipulados.

## Cambios respecto a la versión original (2017)

- Spring Boot 1.5 → 4.1 y Java 8 → 25. Paquete `com.rvillalba.exampleApiHateoas` → `com.rvillalba.hateoas`.
- **No compilaba de forma independiente:** dependía de `clientresttemplate`, un repositorio aparte que ya no existe. Los tests se han reescrito con `MockMvcTester`.
- **El login aceptaba cualquier usuario y contraseña** (había un `TODO` pendiente). Ahora autentica contra un `UserDetailsService` con contraseñas cifradas con BCrypt.
- Unas 25 clases de JWT hechas a mano (filtros, providers, extractores, jjwt 0.6) → `NimbusJwtEncoder` + el *resource server* de Spring Security.
- La cabecera `X-Authorization` pasa a ser la estándar `Authorization: Bearer`. El refresco es ahora `POST` con el token en el cuerpo, en vez de `GET`.
- Se han quitado las dependencias que no se usaban: batch, mail, caché, devtools, joda-time, commons-lang3, MariaDB, actuator-docs y Lombok. El HAL Browser se sustituye por HAL Explorer.

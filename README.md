# bank-core-evolution-lab

[![CI](https://github.com/imfreco/bank-core-evolution-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/imfreco/bank-core-evolution-lab/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Docker](https://img.shields.io/badge/Docker-listo-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-fuente%20de%20verdad-336791)
![MongoDB](https://img.shields.io/badge/MongoDB-modelo%20de%20lectura-47A248)

Laboratorio backend bancario construido con Java 21 y Spring Boot 3 para practicar temas senior de ingeniería backend: transferencias transaccionales, idempotencia, control de concurrencia, auditoría, outbox pattern, SQL como fuente de verdad, MongoDB como modelo de lectura, seguridad, observabilidad, Docker, Kubernetes y diseño de despliegue en AWS.

## Objetivo

El proyecto simula un core bancario pequeño enfocado en transferencias internas entre cuentas. Está pensado como laboratorio técnico: suficientemente compacto para entenderlo completo, pero con suficiente profundidad para defenderlo en una entrevista senior backend.

## Contexto Bancario

El caso de uso central es:

```http
POST /api/v1/transfers
```

El flujo de transferencia debe ser seguro ante reintentos y llamadas concurrentes:

- Validar cuenta origen y cuenta destino.
- Validar estado activo, moneda y saldo disponible.
- Debitar origen y acreditar destino de forma atómica.
- Registrar movimientos financieros append-only.
- Persistir auditoría.
- Persistir un evento outbox dentro de la misma transacción.
- Usar idempotencia persistida para evitar doble débito.

## Tecnologías

- Java 21
- Spring Boot 3.3
- Maven
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Validation
- Spring Security
- Spring Boot Actuator
- Micrometer y Prometheus
- Springdoc OpenAPI
- MongoDB
- Redis opcional
- JUnit 5 y Mockito
- Testcontainers
- Docker Compose
- Manifiestos Kubernetes

## Resumen de Arquitectura

El proyecto está diseñado como un **monolito modular con arquitectura hexagonal por
módulo de negocio**. No se divide en microservicios porque el flujo principal de
transferencias requiere consistencia transaccional fuerte entre cuentas,
movimientos, idempotencia, auditoría y outbox. Separarlo prematuramente convertiría
una transacción local en un flujo distribuido con sagas y compensaciones.

```text
src/main/java/com/imfreco/bank_core_evolution_lab
├── account
│   ├── domain
│   ├── application
│   │   └── port
│   │       ├── in
│   │       └── out
│   └── infrastructure
│       └── adapter
│           ├── in
│           └── out
├── customer
├── transfer
├── movement
├── audit
├── outbox
└── common
```

Cada módulo de negocio separa:

- `domain`: entidades, enums y reglas de negocio.
- `application`: casos de uso, puertos de entrada y puertos de salida.
- `infrastructure`: adaptadores REST, persistencia JPA, MongoDB, jobs y publicación simulada.

Los controladores REST viven en `infrastructure/adapter/in/web`. Los repositorios
Spring Data, MongoDB y jobs viven en `infrastructure/adapter/out` o adaptadores
programados. Los servicios de aplicación dependen de interfaces de puerto, no de
Spring Data ni de DTOs HTTP.

Los diagramas detallados están en [docs/architecture.md](docs/architecture.md).

## Funcionalidades Principales

- Gestión de clientes.
- Creación de cuentas, consulta de saldo y bloqueo preventivo.
- Transferencias internas con idempotencia.
- Movimientos financieros append-only.
- Auditoría técnica y funcional.
- Outbox pattern para publicación simulada de eventos.
- Modelo de lectura en MongoDB para historial de movimientos por cliente.
- Correlation ID con logs MDC.
- Health checks y métricas con Actuator.
- Seguridad demo con login JWT y roles.
- Entorno local con Docker Compose.
- Manifiestos Kubernetes con probes, resources y HPA.
- Notas de despliegue conceptual en AWS.

## Ejecución Local

Requisitos:

- Java 21
- Docker y Docker Compose

Compilar:

```bash
./mvnw clean install
```

Levantar infraestructura y aplicación:

```bash
docker compose up --build
```

La API queda disponible en:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Para consumir endpoints protegidos desde Swagger UI:

1. Ejecuta `POST /api/v1/auth/login` con una de las credenciales demo.
2. Copia el campo `accessToken` de la respuesta.
3. Presiona el botón `Authorize`.
4. Pega el token en el esquema `bearerAuth`.
5. Ejecuta el endpoint normalmente desde `Try it out`.

Todos los endpoints de negocio requieren el header:

```text
Authorization: Bearer <accessToken>
```

## Credenciales Demo

Estas credenciales se siembran por Flyway en las tablas `auth_users` y `auth_user_roles`. Las contraseñas no se guardan en claro: quedan persistidas como hashes bcrypt.

```text
customer / customer123
customer2 / customer2123
operator / operator123
admin    / admin123
```

## Login JWT

Autenticar:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "customer",
  "password": "customer123"
}
```

Respuesta resumida:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "username": "customer",
  "customerId": "11111111-1111-1111-1111-111111111111",
  "roles": ["CUSTOMER"]
}
```

Consumir endpoints protegidos:

```http
Authorization: Bearer <jwt>
```

Roles:

- `ROLE_CUSTOMER`: puede consultar sus propios datos, productos, cuentas y movimientos.
- `ROLE_OPERATOR`: puede bloquear cuentas y consultar auditoría.
- `ROLE_ADMIN`: acceso administrativo.

Nota de producción: este JWT es una implementación demo firmada con HMAC local. Una implementación bancaria debería evolucionar a OAuth2/OIDC, JWT firmado por un IdP, scopes/claims más finos, mTLS, Secrets Manager, rotación de secretos, cifrado, WAF y rate limiting.

## Datos Iniciales

Clientes:

```text
11111111-1111-1111-1111-111111111111
22222222-2222-2222-2222-222222222222
```

Cuentas:

```text
1000000001 ACTIVE  COP 1,000,000
1000000002 ACTIVE  COP   500,000
1000000003 BLOCKED COP   250,000
```

## Pruebas

Pruebas unitarias y verificación por defecto:

```bash
./mvnw test
./mvnw clean verify
```

Pruebas de integración con Testcontainers:

```bash
./mvnw verify -Pintegration-tests
```

Si Docker no está disponible, la clase de integración con Testcontainers está configurada para saltarse de forma controlada.

## Formato

Spotless está configurado para Java y consistencia de espacios/saltos de línea en archivos del proyecto.

Verificar formato:

```bash
./mvnw spotless:check
```

Aplicar formato:

```bash
./mvnw spotless:apply
```

## Ejemplos HTTP

Los ejemplos listos para IntelliJ HTTP Client o VS Code REST Client están en [docs/http-requests](docs/http-requests).

Cubren:

- Login JWT.
- Crear cliente.
- Consultar cliente y productos.
- Crear cuenta.
- Consultar saldo.
- Transferencia exitosa.
- Transferencia con saldo insuficiente.
- Reintento idempotente con misma key y mismo body.
- Conflicto con misma key y body diferente.
- Bloquear cuenta.
- Consultar movimientos.
- Consultar auditoría.
- Health y métricas.

## Endpoints Principales

```text
POST /api/v1/customers
GET  /api/v1/customers/{customerId}
GET  /api/v1/customers/{customerId}/products

POST /api/v1/auth/login

POST /api/v1/accounts
GET  /api/v1/accounts/{accountId}
GET  /api/v1/accounts/{accountId}/balance
POST /api/v1/accounts/{accountId}/block

POST /api/v1/transfers
GET  /api/v1/transfers/{transferReference}

GET  /api/v1/accounts/{accountId}/movements
GET  /api/v1/customers/{customerId}/movements

GET  /api/v1/audit-logs?entityType=&entityId=

GET  /actuator/health
GET  /actuator/metrics
GET  /actuator/prometheus
```

## Idempotencia

`POST /api/v1/transfers` requiere `Idempotency-Key`.

El servicio almacena:

- key de idempotencia;
- hash del request;
- respuesta serializada;
- tipo de operación;
- estado.

Reglas:

- Misma key y mismo body retorna la respuesta original.
- Misma key y body diferente retorna `409 Conflict`.
- La key se persiste en PostgreSQL y está protegida por una restricción única.
- Los inserts duplicados concurrentes se controlan con la restricción de base de datos, no con estado en memoria.

## Patrón Outbox

Cuando una transferencia se completa, la aplicación guarda `TransferCompleted` en `outbox_events` dentro de la misma transacción que el débito, crédito, movimientos y auditoría. `OutboxPublisherJob` simula la publicación y marca eventos como `PUBLISHED`.

Esto demuestra una solución al problema de dual-write: no conviene confirmar el estado de negocio y publicar un evento externo como dos operaciones independientes sin coordinación.

## SQL Como Fuente de Verdad

PostgreSQL es dueño de:

- saldos de cuentas;
- transferencias;
- movimientos financieros;
- registros de idempotencia;
- auditoría;
- eventos outbox.

Esto aporta garantías ACID, constraints, consistencia transaccional y recuperación confiable.

## MongoDB Como Modelo de Lectura

MongoDB almacena `customer_movement_view` como modelo de lectura eventualmente consistente. Es útil para consultas de historial por cliente, pero puede reconstruirse desde PostgreSQL.

Si MongoDB no está disponible o la vista todavía no existe, el endpoint de movimientos puede caer a SQL.

## Observabilidad

El proyecto incluye:

- propagación de `X-Correlation-ID`;
- logs estructurados con MDC;
- payload estándar de error con correlation ID;
- health endpoints de Actuator;
- contadores Micrometer para transferencias exitosas y fallidas;
- métrica para cuentas bloqueadas;
- gauge para eventos outbox pendientes.

## Kubernetes

Los manifiestos están en [k8s](k8s).

Aplicar localmente:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/
```

Comandos útiles:

```bash
kubectl rollout status deployment/bank-core-app -n bank-core
kubectl rollout undo deployment/bank-core-app -n bank-core
```

El deployment de la app incluye readiness probe, liveness probe, requests/limits de recursos y dos réplicas. Los manifiestos locales/demo incluyen PostgreSQL, MongoDB y Redis; en producción deberían reemplazarse por servicios gestionados.

## Despliegue Conceptual en AWS

Ver [docs/aws-architecture.md](docs/aws-architecture.md).

Dirección recomendada para producción:

- ALB o API Gateway.
- EKS o ECS.
- RDS PostgreSQL Multi-AZ.
- MongoDB Atlas o DocumentDB.
- ElastiCache Redis.
- SQS/SNS/MSK para eventos.
- Secrets Manager y KMS.
- CloudWatch, Prometheus y tracing.
- WAF, subnets privadas y security groups estrictos.

## Por Qué Este Proyecto Sirve Para Entrevistas Backend Bancarias

Este proyecto permite explicar temas que suelen diferenciar trabajo backend intermedio de ingeniería backend senior en sistemas financieros:

- límites transaccionales;
- consistencia de saldos;
- control de concurrencia;
- escrituras idempotentes;
- auditoría;
- observabilidad;
- consistencia eventual;
- trade-offs de modelos de lectura;
- operación en Kubernetes;
- arquitectura gestionada en AWS;
- brechas de seguridad productiva y evolución.

## Documentación

- [Diagramas de arquitectura](docs/architecture.md)
- [Guía para defender el proyecto en entrevista](docs/interview-defense.md)
- [Arquitectura AWS](docs/aws-architecture.md)
- [Notas de entrevista](docs/interview-notes.md)
- [Requests HTTP](docs/http-requests)

## Topics Sugeridos Para GitHub

```text
java, spring-boot, backend, banking, postgresql, mongodb, docker, kubernetes, aws, microservices, outbox-pattern, idempotency, testcontainers, observability
```

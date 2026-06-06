# bank-core-evolution-lab

Laboratorio backend bancario construido con Java 21 y Spring Boot 3.x para practicar diseño senior de servicios core: APIs REST, transacciones SQL, idempotencia, concurrencia, auditoría, outbox pattern, MongoDB como read model, seguridad básica, observabilidad, Docker y Kubernetes.

## Objetivos de Aprendizaje

- Implementar casos bancarios con arquitectura limpia/hexagonal simplificada.
- Explicar `@Transactional`, locking, idempotencia y consistencia eventual.
- Separar modelo transaccional en PostgreSQL de vistas de lectura en MongoDB.
- Practicar seguridad, auditoría, trazabilidad y métricas operativas.
- Preparar respuestas de entrevista sobre resiliencia, despliegue cloud y operación bancaria.

## Arquitectura

El paquete base es `com.imfreco.bank_core_evolution_lab`.

- `customer`, `account`, `transfer`, `movement`, `audit`, `outbox`: módulos funcionales.
- `domain`: entidades y enums de negocio.
- `application`: casos de uso y transacciones.
- `infrastructure`: repositorios SQL.
- `web`: DTOs, mappers y controladores REST.
- `movement.mongo`: read model en MongoDB.
- `common`: errores, seguridad, logging, métricas e idempotencia.

No se exponen entidades JPA en controladores. Los controladores reciben DTOs, delegan en servicios de aplicación y retornan responses explícitos.

## Stack

Java 21, Spring Boot 3.3, Maven, Spring Web, Spring Data JPA, PostgreSQL, Flyway, Spring Validation, Spring Security, Actuator, Micrometer/Prometheus, Springdoc OpenAPI, MongoDB, Redis opcional, JUnit 5, Mockito y Testcontainers.

## Ejecución Local

Requisitos: Java 21, Docker y Docker Compose.

```bash
./mvnw clean install
docker compose up --build
```

La app queda en `http://localhost:8080`.

Credenciales Basic Auth de laboratorio:

- `customer / customer123`
- `operator / operator123`
- `admin / admin123`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Pruebas

Unitarias:

```bash
./mvnw test
```

Integración con Testcontainers:

```bash
./mvnw verify -Pintegration-tests
```

## Datos Seed

Clientes:

- `11111111-1111-1111-1111-111111111111`
- `22222222-2222-2222-2222-222222222222`

Cuentas:

- `1000000001`: activa, COP, saldo 1.000.000
- `1000000002`: activa, COP, saldo 500.000
- `1000000003`: bloqueada, COP

## Endpoints Principales

- `POST /api/v1/customers`
- `GET /api/v1/customers/{customerId}`
- `GET /api/v1/customers/{customerId}/products`
- `POST /api/v1/accounts`
- `GET /api/v1/accounts/{accountId}`
- `GET /api/v1/accounts/{accountId}/balance`
- `POST /api/v1/accounts/{accountId}/block`
- `POST /api/v1/transfers`
- `GET /api/v1/transfers/{transferReference}`
- `GET /api/v1/accounts/{accountId}/movements`
- `GET /api/v1/customers/{customerId}/movements`
- `GET /api/v1/audit-logs?entityType=&entityId=`
- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## Ejemplo de Transferencia

```bash
curl -u customer:customer123 \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: transfer-demo-001" \
  -H "X-Correlation-ID: demo-correlation-001" \
  -H "X-Channel: WEB" \
  -d '{
    "sourceAccountNumber": "1000000001",
    "targetAccountNumber": "1000000002",
    "amount": 10000.00,
    "currency": "COP"
  }' \
  http://localhost:8080/api/v1/transfers
```

Si repites la misma petición con la misma `Idempotency-Key`, retorna la misma referencia y evita un segundo débito. Si reutilizas la misma key con otro body, retorna `409 Conflict`.

## Idempotencia

`POST /api/v1/transfers` requiere el header `Idempotency-Key`. El servicio calcula un hash SHA-256 del body serializado y guarda:

- key;
- hash de request;
- respuesta exitosa;
- operación;
- estado.

La creación del registro idempotente ocurre en la misma transacción que la transferencia. Ante timeout del cliente, un retry con el mismo body recupera la respuesta ya comprometida y no vuelve a debitar.

## Transferencias y Concurrencia

La transferencia se ejecuta dentro de `@Transactional`. El repositorio de cuentas usa `PESSIMISTIC_WRITE` para bloquear ambas cuentas en orden estable por número de cuenta. Esto reduce riesgo de doble débito y deadlocks en transferencias cruzadas. `@Version` queda como defensa adicional para flujos de menor contención donde optimistic locking sea suficiente.

## Outbox Pattern

Cuando una transferencia se completa o una cuenta se bloquea, se crea un evento en `outbox_events` dentro de la misma transacción SQL. `OutboxPublisherJob` simula la publicación y marca eventos como `PUBLISHED`. En producción, ese job publicaría en Kafka, RabbitMQ, SNS/SQS u otro broker.

## SQL y MongoDB

PostgreSQL es la fuente de verdad para saldos, transferencias, movimientos, auditoría e idempotencia. MongoDB almacena `customer_movement_view` como vista de lectura eventualmente consistente. Si Mongo no tiene proyección disponible, el endpoint de movimientos de cliente cae a SQL.

## Seguridad

La seguridad actual usa Basic Auth e in-memory users para laboratorio. Roles:

- `ROLE_CUSTOMER`: consulta productos, saldos, movimientos y realiza transferencias.
- `ROLE_OPERATOR`: bloquea cuentas y consulta auditoría.
- `ROLE_ADMIN`: acceso administrativo.

Evolución natural: OAuth2/OIDC con JWT, scopes por producto, claims de cliente, autorización ABAC y auditoría enriquecida.

## Observabilidad

Cada request tiene `X-Correlation-ID`; si no llega, se genera uno y se agrega al MDC de logs y respuestas de error. Métricas custom:

- `bank.transfers.successful`
- `bank.transfers.failed`
- `bank.accounts.blocked`
- `bank.outbox.events.pending`

## Kubernetes

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/
kubectl rollout status deployment/bank-core-app -n bank-core
kubectl rollout undo deployment/bank-core-app -n bank-core
```

Readiness indica si el pod puede recibir tráfico. Liveness indica si debe reiniciarse. Secrets no deben ir en ConfigMap; en producción usar Secrets Manager/External Secrets/KMS.

## Preguntas de Entrevista

- Como evitas doble débito si el cliente reintenta por timeout?
- Por qué el outbox debe guardarse en la misma transacción que la transferencia?
- Cuándo usar optimistic locking vs pessimistic locking?
- Cómo reconciliarías MongoDB si una proyección falla?
- Qué métricas alertarías en transferencias internas?
- Cómo diseñarías autorización real para que un cliente solo vea sus productos?
- Qué RTO/RPO propondrías para un core bancario?

Ver también:

- `docs/interview-notes.md`
- `docs/aws-architecture.md`

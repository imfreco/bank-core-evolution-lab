# Notas de Entrevista

## @Transactional

Define un límite transaccional. En este proyecto protege transferencias: débito, crédito, movimientos, auditoría, outbox e idempotencia se confirman o revierten juntos. Las transacciones deben ser cortas y no incluir llamadas externas.

## Optimistic Locking

Usa una columna `version` y falla si otro proceso actualizó la fila antes del commit. Sirve cuando la contención es baja y prefieres detectar conflictos en vez de bloquear antes.

## Pessimistic Locking

Bloquea filas durante la transacción. En transferencias se usa `PESSIMISTIC_WRITE` sobre cuentas porque el saldo es crítico y los reintentos/concurrencia pueden causar doble débito si no se coordina.

## Idempotencia

Permite repetir una operación sin repetir su efecto. `Idempotency-Key` + hash del body evita doble débito y detecta reutilización peligrosa de la misma key con otro payload.

## Patrón Outbox

Guarda eventos en una tabla dentro de la misma transacción de negocio. Un publisher asíncrono los publica después. Evita el problema de confirmar DB pero fallar al publicar el evento.

## Consistencia Eventual

MongoDB puede estar desactualizado por segundos respecto a PostgreSQL. Es aceptable para lectura de historial si se comunica el modelo, se monitorea lag y existe reconciliación.

## SQL vs MongoDB

SQL es ideal para saldos, integridad referencial, transacciones ACID y auditoría fuerte. MongoDB es útil para vistas agregadas de lectura y consultas optimizadas por documento.

## Liveness vs Readiness

Readiness retira el pod del balanceador si no está listo para tráfico. Liveness reinicia el contenedor si está atascado. Una liveness mal configurada puede causar reinicios en cascada.

## CI/CD

Pipeline mínimo: build, unit tests, integration tests con Testcontainers, análisis estático, build de imagen, escaneo de vulnerabilidades, push a registry, deploy progresivo, smoke tests y rollback automático.

## Monitoreo

Alertas clave: tasa de transferencias fallidas, latencia p95/p99, errores 5xx, locks/timeout DB, outbox pendiente, lag de proyección Mongo, saturación de pool JDBC, CPU/memoria y reinicios de pods.

## Seguridad Bancaria

OAuth2/OIDC, JWT firmados, autorización por scopes/claims, cifrado en tránsito y reposo, secretos en vault, auditoría inmutable, mínimo privilegio, WAF, rate limiting, detección de fraude y segregación de funciones.

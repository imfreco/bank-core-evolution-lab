# Guía Para Defender El Proyecto En Entrevista

## 1. ¿Qué problema resuelve este proyecto?

Modela un backend core bancario pequeño enfocado en transferencias internas. El objetivo no es construir un banco completo, sino demostrar cómo diseñar flujos críticos con consistencia transaccional, idempotencia, auditoría, observabilidad y preparación para despliegue.

## 2. ¿Por qué PostgreSQL es la fuente de verdad?

Los saldos, transferencias y movimientos financieros requieren transacciones ACID, constraints, índices y consistencia fuerte. PostgreSQL es el lugar más seguro del proyecto para débitos, créditos, registros de idempotencia, auditoría y eventos outbox.

## 3. ¿Por qué MongoDB se usa como modelo de lectura?

MongoDB almacena una vista de movimientos centrada en el cliente y optimizada para consulta. No es la autoridad sobre los saldos. Es una proyección reconstruible desde SQL, lo cual representa un trade-off típico de CQRS para simplificar o acelerar lecturas.

## 4. ¿Cómo se evita el doble débito?

`POST /api/v1/transfers` requiere `Idempotency-Key`. La key se persiste en PostgreSQL junto con un hash del request y la respuesta serializada. Si el cliente reintenta con la misma key y el mismo body, se retorna la respuesta original en lugar de ejecutar el débito nuevamente.

## 5. ¿Cómo funciona la idempotencia?

El servicio calcula un hash del body y consulta `idempotency_records`. Si la key es nueva, crea el registro y continúa. Si la key ya existe con el mismo hash y una respuesta completada, devuelve la respuesta almacenada. Si la misma key se usa con otro body, retorna `409 Conflict`.

## 6. ¿Qué pasa si dos requests llegan con la misma Idempotency-Key al mismo tiempo?

La restricción única de base de datos sobre `idempotency_key` es la defensa final. Solo una transacción puede crear el registro de idempotencia. La petición competidora recibe un conflicto controlado mientras la primera completa, por lo que el pipeline de transferencia no se ejecuta dos veces.

## 7. ¿Por qué usar outbox pattern?

El outbox pattern resuelve el problema de dual-write. La transferencia y su evento se confirman en la misma transacción SQL. Luego, un publicador separado envía o simula el envío del evento. Así se evita confirmar movimiento de dinero y perder el evento de integración.

## 8. ¿Cómo se maneja la consistencia eventual?

PostgreSQL confirma primero. MongoDB se actualiza después como proyección. La API puede caer a SQL cuando el modelo de lectura no está disponible. En producción, deberían existir jobs de reconciliación para reconstruir o verificar proyecciones desde SQL.

## 9. ¿Cómo se registra auditoría?

Las operaciones críticas crean registros en `audit_logs` con tipo de operación, entidad, actor, canal, correlation ID y detalles estructurados. Esto da trazabilidad para transferencias y bloqueos de cuenta sin exponer stack traces internos al cliente.

## 10. ¿Cómo se maneja la trazabilidad con correlation ID?

`CorrelationIdFilter` lee `X-Correlation-ID` o genera uno. Lo guarda en MDC para logs y lo retorna en las respuestas. El mismo ID se incluye en auditoría y payloads de outbox para facilitar troubleshooting entre sistemas.

## 11. ¿Cómo se monitorea el sistema?

Spring Boot Actuator expone health y métricas. Micrometer registra contadores para transferencias exitosas, transferencias fallidas y bloqueos de cuenta. También existe un gauge para eventos outbox pendientes. Prometheus puede consumir `/actuator/prometheus`.

## 12. ¿Cómo se desplegaría en Kubernetes?

La app corre como Deployment con dos réplicas, requests/limits de recursos, readiness probe y liveness probe. ConfigMap almacena configuración no sensible y Secret almacena credenciales. HPA puede escalar pods con base en CPU o métricas de negocio.

## 13. ¿Cómo se llevaría a AWS?

Usaría ALB o API Gateway en la entrada, EKS o ECS para cómputo, RDS PostgreSQL Multi-AZ para datos transaccionales, MongoDB Atlas o DocumentDB para el modelo de lectura, ElastiCache para Redis, SQS/SNS o MSK para eventos, Secrets Manager y KMS para secretos/cifrado, CloudWatch y Prometheus para observabilidad, y WAF para protección.

## 14. ¿Qué mejorarías para producción?

- Reemplazar Basic Auth por OAuth2/OIDC y JWT firmado.
- Validar ownership del cliente antes de retornar datos de customer/account/movement.
- Agregar mTLS entre servicios internos.
- Agregar rate limiting, validaciones antifraude y autorización más fuerte.
- Agregar auditoría inmutable y políticas de retención.
- Reemplazar el outbox simulado por un broker real.
- Agregar reconciliación y alertas por lag de proyección en MongoDB.
- Agregar pruebas de performance y resiliencia.

TODO production-grade: validar ownership del cliente autenticado antes de retornar datos de customer/account/movement.

## 15. ¿Qué trade-offs tiene esta solución?

El diseño prioriza claridad y valor para entrevista sobre complejidad productiva completa. Basic Auth es intencionalmente simple. La proyección MongoDB es eventualmente consistente. El publicador outbox es simulado. El bloqueo pesimista mejora la seguridad sobre saldos, pero puede reducir throughput bajo alta contención.

## Elevator Pitch Del Proyecto En 90 Segundos

`bank-core-evolution-lab` es un backend en Spring Boot 3 y Java 21 que simula un flujo crítico de transferencias bancarias internas. El caso principal es una transferencia entre cuentas implementada con transacciones PostgreSQL, bloqueo pesimista, movimientos append-only, auditoría e idempotencia persistida mediante `Idempotency-Key`. También usa outbox pattern para que una transferencia completada cree un evento de integración de forma atómica con los datos de negocio. PostgreSQL es la fuente de verdad, mientras MongoDB funciona como read model eventualmente consistente para historial de movimientos por cliente. El proyecto incluye Spring Security, métricas Actuator, correlation IDs, Docker Compose, manifiestos Kubernetes, pruebas con Testcontainers y documentación de despliegue en AWS. Está diseñado para demostrar temas senior como concurrencia, consistencia, observabilidad, operación y trade-offs cloud en un contexto bancario.

# AWS Architecture

## Vista General

Para un banco, este backend se desplegaría en una VPC con subnets públicas y privadas, multi-AZ, cifrado por defecto, observabilidad centralizada y separación estricta entre capa de entrada, cómputo, datos y mensajería.

## Entrada

- API Gateway o Application Load Balancer para exponer APIs HTTPS.
- AWS WAF para reglas OWASP, rate limiting y protección básica ante abuso.
- ACM para certificados TLS.
- Autenticación corporativa con OAuth2/OIDC y un IdP como Cognito, Auth0, Keycloak o proveedor bancario interno.

## Cómputo

- EKS si el banco ya opera Kubernetes y necesita control fino de networking, sidecars, service mesh y despliegues progresivos.
- ECS Fargate si se busca menor carga operativa.
- Imágenes en ECR con escaneo de vulnerabilidades.
- Deploy rolling, blue/green o canary con rollback automático por alarmas.

## Datos

- RDS PostgreSQL Multi-AZ como fuente transaccional.
- Backups automáticos, PITR, cifrado KMS, réplicas de lectura si aplica.
- MongoDB Atlas o Amazon DocumentDB para read model de movimientos.
- ElastiCache Redis para cache, rate limiting o locks no críticos.
- Nunca usar Redis como fuente de verdad de saldos.

## Eventos

- SQS para colas simples y desacoplamiento robusto.
- SNS para fan-out.
- MSK/Kafka si se requiere streaming de alto volumen y replay.
- Outbox pattern evita perder eventos cuando la base confirma pero el broker falla.

## Secretos y Cifrado

- AWS Secrets Manager para credenciales de DB y tokens.
- KMS para cifrado de RDS, EBS, logs y secretos.
- Rotación de credenciales con ventanas controladas.
- IAM roles por servicio, sin claves estáticas en contenedores.

## Networking

- Subnets públicas solo para ALB/NAT.
- Servicios, RDS, DocumentDB y Redis en subnets privadas.
- Security Groups con mínimo privilegio: app -> DB, app -> Redis, app -> broker.
- VPC endpoints para servicios AWS cuando se quiera evitar salida pública.

## Observabilidad

- CloudWatch Logs para logs estructurados con `correlationId`.
- CloudWatch Metrics y Container Insights para CPU, memoria, restarts y red.
- Prometheus/Grafana en EKS o Amazon Managed Prometheus.
- X-Ray u OpenTelemetry para trazas distribuidas.
- Alarmas por 5xx, latencia p99, outbox pendiente, errores de transferencia y saturación DB.

## Auditoría

- CloudTrail para actividad AWS.
- AuditLog funcional en PostgreSQL para operaciones críticas.
- Retención y almacenamiento WORM/S3 Object Lock para evidencias regulatorias si aplica.

## Alta Disponibilidad

- Multi-AZ en ALB, EKS/ECS, RDS, Redis y DocumentDB/Atlas.
- Mínimo dos réplicas de app.
- HPA por CPU y métricas de negocio.
- Readiness probes para sacar pods no listos sin reiniciarlos innecesariamente.

## Backups y DR

- RDS PITR y snapshots probados.
- Backups de Mongo/DocumentDB.
- Runbooks de restauración.
- DR básico: warm standby en otra región para sistemas críticos.
- RTO/RPO ejemplo para laboratorio: RTO 2 horas, RPO 15 minutos. En core real podría ser mucho más estricto según criticidad.

## Operación Bancaria

- Segregación de ambientes y cuentas AWS.
- Aprobaciones para cambios productivos.
- Escaneo SAST/DAST/dependencias.
- Políticas de retención de logs.
- Pruebas de resiliencia y game days.
- Reconciliación periódica entre outbox, SQL y read models.

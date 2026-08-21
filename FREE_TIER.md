# NearNow — ₹0 Development Cost Policy

This is the authoritative cost boundary for the supplied development build.

## Included and free when self-hosted locally

| Component | How NearNow uses it | Development bill |
|---|---|---:|
| Spring Boot | Local JVM / Docker | ₹0 |
| PostgreSQL | `pgvector/pgvector:pg16` Docker container | ₹0 |
| pgvector | PostgreSQL extension | ₹0 |
| Redis | `redis:7-alpine` Docker container | ₹0 |
| Flutter | Local SDK/device/emulator | ₹0 |
| Next.js | Local Node process | ₹0 |
| Local semantic search | Java deterministic embeddings + pgvector | ₹0 |
| Mock payment | Local Spring Boot code | ₹0 |
| Notifications | Local async log/event | ₹0 |
| Prometheus/Grafana | Optional local Docker profile | ₹0 |

## Deliberately not used

- OpenAI API
- Razorpay API
- Firebase services
- AWS ElastiCache / managed Redis
- Cloud PostgreSQL
- RabbitMQ hosted service
- Celery worker service
- Twilio
- SendGrid
- Firebase Cloud Messaging
- Any mandatory paid SaaS/API credential

## Important distinction

Redis and PostgreSQL are software. Running them yourself in Docker does not create a vendor cloud bill. A managed cloud service such as AWS ElastiCache would be a different choice and can cost money.

Likewise, this project contains a payment **simulation**, not a live payment provider. No real transaction can be charged by the default configuration.

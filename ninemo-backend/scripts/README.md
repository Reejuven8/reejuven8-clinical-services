# Running the backend locally

Databases/brokers run in Docker; the Java services run as plain `java -jar` processes
on the host. (`infrastructure/docker-compose.yml` — the full-stack one that also builds
service images — is internally inconsistent: its `./services/*` build contexts don't
resolve from `infrastructure/`, where its volume paths do. Use infra + jars instead.)

## From Android Studio

Run configurations live in `ninemo-mobile/.run/` and appear in the run-configuration
dropdown when `ninemo-mobile` is open. Order matters:

1. **Backend · 0 infra (docker)** — start this first (Postgres, Mongo, Redis, Kafka, RabbitMQ)
2. **Backend · F6 stack** — gateway + identity + community, or pick individual services
3. **Backend · all Java services** — everything except the Python parser

Stop a service with the red stop button. Each runs in its own Run tool-window tab.

## From the terminal

```bash
scripts/infra-up.sh                              # databases + brokers
scripts/run-service.sh identity-abha-service     # one service
scripts/run-service.sh api-gateway --build       # force a rebuild first
scripts/run-ai-parsing.sh                        # Python/FastAPI service
scripts/infra-down.sh                            # stop infra   (-v also wipes volumes)
```

`run-service.sh` with no arguments lists every service, its port, and its datastores.

## Ports

| Port | Service | Datastores |
|---|---|---|
| 8080 | api-gateway | Redis |
| 8081 | identity-abha-service | PostgreSQL + Redis |
| 8082 | health-data-service | MongoDB + S3 |
| 8083 | ai-parsing-service (Python) | — |
| 8084 | ninemo-clinical-service | PostgreSQL + MongoDB |
| 8085 | notification-service | PostgreSQL |
| 8086 | ninemo-community-service | MongoDB |

## Notes

- **JDK**: the backend targets Java 26 via Homebrew `openjdk@26`, which is *not* linked
  into `/Library/Java/JavaVirtualMachines` — `/usr/libexec/java_home` and the
  `/usr/bin/java` stub cannot find it. `run-service.sh` resolves `JAVA_HOME` itself;
  export your own to override.
- **No env vars needed**: every service's `application.yml` defaults to `localhost`
  plus the compose credentials.
- **Port already in use**: the script refuses to start and prints the holding PID.
- **Mobile on a physical device**: the app talks to the gateway over
  `adb reverse tcp:8080 tcp:8080` (and `tcp:8086` for STOMP chat). An emulator uses
  `10.0.2.2` instead — see `PlatformConfig` in `HttpEngine.android.kt`.

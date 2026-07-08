# Cabsy Ride Service

Handles ride requests and their lifecycle (`REQUESTED` → `MATCHED` → `ONGOING`
→ `COMPLETED`, or `CANCELLED`) for the Cabsy ride-booking platform. Publishes
ride events to Kafka for the Matching Service (and other consumers) to react
to. This service owns ride state only — it has no knowledge of user
credentials or driver-matching logic.

## Tech stack

- Java 21, Spring Boot 4.1.0, Maven
- Spring Web, Spring Data JPA, Spring Validation, Spring for Apache Kafka
- PostgreSQL
- Lombok

## Prerequisites

- Java 21
- PostgreSQL reachable at the URL in `application.yml` (defaults to
  `localhost:5434`, db `cabsy_ride`, user/pass `cabsy`/`cabsy`)
- A Kafka broker reachable at `localhost:29092` (only required for event
  publishing — see [Kafka events](#kafka-events) below)

Create the database:

```sql
CREATE DATABASE cabsy_ride;
```

> **No `docker-compose.yml` currently exists in this workspace.** Postgres
> and Kafka need to be provisioned some other way (local installs, or your
> own compose file) until one is added.

## Running locally

```bash
./mvnw spring-boot:run
```

The service starts on **port 8082**.

Datasource, JPA, and Kafka settings (bootstrap servers, topic names) are
configured in `src/main/resources/application.yml`.

## API

| Method | Path                       | Description                          |
|--------|----------------------------|---------------------------------------|
| POST   | `/api/v1/rides`            | Request a ride                       |
| GET    | `/api/v1/rides/{id}`       | Fetch a ride by id                   |
| GET    | `/api/v1/rides?riderId=…`  | List rides for a rider               |
| PATCH  | `/api/v1/rides/{id}/status`| Transition a ride's status           |

### Request a ride

```bash
curl -X POST http://localhost:8082/api/v1/rides \
  -H "Content-Type: application/json" \
  -d '{
    "riderId": "rider-123",
    "pickupLat": 37.7749,
    "pickupLng": -122.4194,
    "dropoffLat": 37.8044,
    "dropoffLng": -122.2712
  }'
```

Publishes a `ride-requested` event and returns the ride with status
`REQUESTED`.

### Fetch a ride

```bash
curl http://localhost:8082/api/v1/rides/{id}
```

### List rides for a rider

```bash
curl "http://localhost:8082/api/v1/rides?riderId=rider-123"
```

### Update ride status

```bash
# REQUESTED -> MATCHED (driverId required)
curl -X PATCH http://localhost:8082/api/v1/rides/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "MATCHED", "driverId": "driver-456"}'

# MATCHED -> ONGOING
curl -X PATCH http://localhost:8082/api/v1/rides/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "ONGOING"}'

# ONGOING -> COMPLETED
curl -X PATCH http://localhost:8082/api/v1/rides/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

Each successful transition publishes a `ride-status-changed` event.

### Valid status transitions

| From        | To                          |
|-------------|------------------------------|
| `REQUESTED` | `MATCHED`, `CANCELLED`       |
| `MATCHED`   | `ONGOING`, `CANCELLED`       |
| `ONGOING`   | `COMPLETED`                  |
| `COMPLETED` | — (terminal)                 |
| `CANCELLED` | — (terminal)                 |

Any other transition returns `409 Conflict`. Transitioning to `MATCHED`
without a `driverId` also returns `409`.

## Kafka events

| Topic                  | Published when              | Payload                                                                 |
|-------------------------|------------------------------|--------------------------------------------------------------------------|
| `ride-requested`         | A ride is created            | `rideId, riderId, pickupLat, pickupLng, dropoffLat, dropoffLng, requestedAt` |
| `ride-status-changed`    | A ride's status transitions  | `rideId, riderId, driverId, oldStatus, newStatus, changedAt`             |

Events are keyed by `rideId` and serialized as JSON.

Publishing is best-effort: if Kafka is unreachable, the ride is still saved
and the HTTP call still succeeds — the failure is logged as a warning
(`RideEventPublisher`) rather than silently dropped or surfaced to the
caller.

## Error responses

Errors return a consistent JSON body:

```json
{
  "timestamp": "2026-07-08T10:12:03.512091Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ride not found: 3f9a1b2c-..."
}
```

| Status | Cause                                                      |
|--------|--------------------------------------------------------------|
| 400    | Validation failure (e.g. missing/out-of-range coordinates) |
| 404    | Ride id does not exist                                     |
| 409    | Illegal status transition, or `MATCHED` without `driverId` |

# Notification Service

A Spring Boot service that broadcasts notifications to users using **Apache Kafka** sharding — without ever scanning the full database at once.

---

## How It Works

Instead of reading all users in one query, the service:

1. Counts total users and divides them into fixed-size **shards** (default: 10,000 users/shard)
2. Publishes one lightweight Kafka message per shard — returns `202 Accepted` immediately
3. Consumer threads pick up shards in parallel, each fetching only their slice from the DB

```
POST /broadcast → count users → publish 100 shard messages → return 202
                                        ↓
                          Kafka (10 partitions, 3 consumer threads)
                                        ↓
                       fetch 10k users → send notification → repeat
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1 |
| Language | Java 17 |
| Messaging | Apache Kafka (KRaft mode) |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Containerisation | Docker |

---

## Prerequisites

- Docker
- Java 17+
- Maven (or use the included `./mvnw` wrapper)

---

## Quick Start

### 1. Clone and configure

```bash
cp notification/.env.example notification/.env
# Edit .env and set DB_PASSWORD (and any other values you want to override)
```

### 2. Start Kafka

```bash
docker compose up -d
```

Kafka UI will be available at **http://localhost:8090**

### 3. Run the app

```bash
cd notification
set -a && source .env && set +a && ./mvnw spring-boot:run
```

### 4. Send a broadcast notification

```bash
curl -s -X POST http://localhost:8080/api/v1/notifications/broadcast \
  -H "Content-Type: application/json" \
  -d '{"title": "Hello", "message": "Test broadcast", "channel": "EMAIL"}' | jq .
```

**Response:**
```json
{
  "jobId": "550e8400-...",
  "totalShards": 100,
  "totalUsers": 1000000,
  "status": "SCHEDULED",
  "message": "Notification job scheduled. 100 shards dispatched."
}
```

---

## API

### `POST /api/v1/notifications/broadcast`

Schedules a notification broadcast to all users.

**Request body:**

```json
{
  "title": "string (required)",
  "message": "string (required)",
  "channel": "EMAIL | SMS | PUSH_NOTIFICATION (required)"
}
```

**Response:** `202 Accepted`

```json
{
  "jobId": "uuid",
  "totalShards": 100,
  "totalUsers": 1000000,
  "status": "SCHEDULED",
  "message": "..."
}
```

---

## Configuration

All configuration is driven by environment variables. Copy `notification/.env.example` to `notification/.env` to get started.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/notification` | Database JDBC URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | — | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `KAFKA_CONSUMER_GROUP` | `notification-group` | Consumer group ID |
| `KAFKA_NOTIFICATION_TOPIC` | `notification-shards` | Kafka topic name |
| `KAFKA_TOPIC_PARTITIONS` | `10` | Number of topic partitions |
| `NOTIFICATION_BATCH_SIZE` | `10000` | Users per shard |
| `SERVER_PORT` | `8080` | HTTP port |

---

## Project Structure

```
Notification/
├── docker-compose.yml          # Kafka + Kafka UI
├── notification/               # Spring Boot application
│   ├── .env.example            # Environment variable template
│   └── src/main/java/com/saurabh/notification/
│       ├── config/             # Kafka, Jackson configuration
│       ├── controller/         # REST endpoints
│       ├── service/            # Orchestration, Kafka producer
│       ├── consumer/           # Kafka consumer (shard processor)
│       ├── repository/         # JPA repositories
│       ├── entity/             # JPA entities
│       ├── dto/                # Request/response objects
│       ├── mapper/             # Entity ↔ DTO mapping
│       └── exception/          # Global error handling
```

---

## Database Schema

```sql
CREATE TABLE users (
    id    BIGSERIAL PRIMARY KEY,
    name  TEXT NOT NULL,
    email TEXT NOT NULL
);
```

---

## Extending

To plug in a real notification provider, implement the `sendNotification` method in `NotificationConsumer.java`:

```java
private void sendNotification(UserDTO user, UserShardMessage shardMessage) {
    switch (shardMessage.getNotificationRequest().getChannel()) {
        case EMAIL            -> emailService.send(user.getEmail(), ...);
        case SMS              -> smsService.send(...);
        case PUSH_NOTIFICATION -> pushService.send(...);
    }
}
```
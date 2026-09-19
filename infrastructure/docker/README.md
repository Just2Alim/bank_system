# Local Banking Topology

This Compose topology starts isolated databases for Orda, Nomad, and Tengri plus a local Redpanda Kafka broker.

```bash
docker compose -f infrastructure/docker/compose.yaml up --build
```

Published services:

- Orda legacy core simulator: `http://localhost:8081`
- Nomad native bank platform: `http://localhost:8082`
- Tengri native bank platform: `http://localhost:8083`
- Redpanda Kafka: `localhost:9092`

The three banks intentionally use separate PostgreSQL databases. Interbank movement must go through the simulated payment platform instead of direct cross-bank table access.

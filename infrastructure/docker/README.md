# Local Banking Topology

This Compose topology starts isolated databases for Orda, Nomad, and Tengri plus a local Redpanda Kafka broker.

```bash
docker compose -f infrastructure/docker/compose.yaml up --build
```

Published services:

- Interactive web console: `http://localhost:5173`
- Open-banking gateway: `http://localhost:8080`
- Orda legacy core simulator: `http://localhost:8081`
- Nomad native bank platform: `http://localhost:8082`
- Tengri native bank platform: `http://localhost:8083`
- Redpanda Kafka: `localhost:9092`

The three banks intentionally use separate PostgreSQL databases. Interbank movement must go through the simulated payment platform instead of direct cross-bank table access.

## BCC Financial API sandbox

The gateway starts in deterministic `demo` mode, so the Accounts and New transfer screens work immediately. Copy `.env.example` to `.env` and set the following values only after obtaining them from the BCC developer application:

```dotenv
OPEN_BANKING_PROVIDER=bcc
BCC_CLIENT_ID=...
BCC_CLIENT_SECRET=...
BCC_APP_ID=...
```

Then restart `open-banking-gateway`. BCC mode reads sandbox accounts and recent statements. Writes remain intentionally disabled until the OpenAPI document for the exact subscribed payment product and its approval/signing flow are verified. OAuth tokens and the client secret stay in the gateway and are never sent to the browser.

Health and provider checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/integration/status
```

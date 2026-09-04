# C4 Container Architecture

> Classification: **SIMULATOR_DESIGN_CHOICE**. Container names describe this repository's fictional implementation and are not assertions about private systems used by real institutions.

## Application Containers

```mermaid
flowchart TB
    Browser["Banking Console\nReact + TypeScript"]
    Gateway["Gateway / BFF\nSpring Cloud Gateway"]
    Identity["Keycloak\nOIDC provider"]

    subgraph BankA[Orda Bank - legacy/hybrid]
        Orda["bank-platform: orda\ndigital workflow and router"]
        OrdaDB[(bank_orda_edge)]
        Legacy["legacy-core-simulator\naccounts, holds, ledger"]
        LegacyDB[(bank_orda_core)]
        CardA["card-processing: orda"]
        CardADB[(card_orda)]
        Orda --> OrdaDB
        Orda -->|CoreBankingPort over authenticated HTTP| Legacy
        Legacy --> LegacyDB
        CardA --> CardADB
        CardA <-->|idempotent hold/capture commands| Orda
    end

    subgraph BankB[Nomad Bank - digital-native]
        Nomad["bank-platform: nomad\nworkflow plus native core"]
        NomadDB[(bank_nomad)]
        CardB["card-processing: nomad"]
        CardBDB[(card_nomad)]
        Nomad --> NomadDB
        CardB --> CardBDB
        CardB <-->|idempotent hold/capture commands| Nomad
    end

    subgraph BankC[Tengri Bank - hybrid]
        Tengri["bank-platform: tengri\nworkflow plus hybrid core"]
        TengriDB[(bank_tengri)]
        CardC["card-processing: tengri"]
        CardCDB[(card_tengri)]
        Tengri --> TengriDB
        CardC --> CardCDB
        CardC <-->|idempotent hold/capture commands| Tengri
    end

    NPP["national-payment-platform\nISO, aliases, MSMP, RTGS, clearing"]
    NPPDB[(national_payments)]
    Simulation["simulation-engine\nclock, profiles, normal activity"]
    SimulationDB[(simulation)]
    Ops["operations-query\nprojections and resumable SSE"]
    OpsDB[(operations)]
    Kafka[(Kafka)]
    Redis[(Redis\nephemeral only)]
    OTel["OpenTelemetry Collector"]
    Observability["Prometheus + Grafana\nTempo + Loki"]

    Browser <-->|same-origin HTTPS| Gateway
    Gateway <-->|OIDC code + PKCE| Identity
    Gateway --> Orda
    Gateway --> Nomad
    Gateway --> Tengri
    Gateway --> NPP
    Gateway --> Simulation
    Gateway --> Ops
    Gateway --> Redis

    Orda <-->|ISO payment submission/status| NPP
    Nomad <-->|ISO payment submission/status| NPP
    Tengri <-->|ISO payment submission/status| NPP
    NPP --> NPPDB
    Simulation --> SimulationDB
    Ops --> OpsDB

    Orda --> Kafka
    Nomad --> Kafka
    Tengri --> Kafka
    Legacy --> Kafka
    CardA --> Kafka
    CardB --> Kafka
    CardC --> Kafka
    NPP --> Kafka
    Simulation --> Kafka
    Kafka --> Orda
    Kafka --> Nomad
    Kafka --> Tengri
    Kafka --> Ops

    Gateway --> OTel
    Orda --> OTel
    Nomad --> OTel
    Tengri --> OTel
    Legacy --> OTel
    CardA --> OTel
    CardB --> OTel
    CardC --> OTel
    NPP --> OTel
    Simulation --> OTel
    Ops --> OTel
    OTel --> Observability
```

## Container Responsibilities

### Gateway / BFF

- Terminates browser authentication and enforces role/bank scope.
- Uses OIDC authorization-code plus PKCE and server-side token storage.
- Issues Secure, HttpOnly, SameSite cookies; mutations require CSRF protection.
- Routes commands to one bank based on authenticated claims, not arbitrary payload data.
- Proxies the operations SSE endpoint so browser clients do not need bearer-token query parameters.
- Contains no payment orchestration or financial state.

### Bank Platform

- Owns customer/channel metadata, devices, sessions, transfer intent, workflow history, payment routing, and bank audit events.
- Implements one `CoreBankingPort`; Orda uses a remote adapter while Nomad and Tengri use local adapters.
- Creates stable command, correlation, causation, end-to-end, and idempotency identifiers.
- Routes same-bank payments internally and different-bank payments only through the NPP simulator.
- Does not access card, NPP, or another bank's database.

### Financial Core

- Owns account products, account lifecycle, customer-deposit subledgers, holds, immutable journals, balance projections, and statements.
- Orda runs it in `legacy-core-simulator`; Nomad/Tengri run it as an in-process module.
- Exposes domain-specific commands, never a generic “post arbitrary journal” endpoint.
- Commits financial effect and outbox record atomically.

### Card Processor

- Owns synthetic card identifiers, lifecycle, limits, merchants, terminals, authorizations, captures, and reversals.
- Calls the bank's idempotent core boundary to create/release/capture a hold.
- Never stores real-looking PANs, CVVs, PINs, or magnetic-stripe data.
- Has one isolated deployment and database per fictional bank.

### National Payment Platform

- Owns participants, routing, aliases, ISO-message validation/history, RTGS queues, clearing cycles, and the authoritative participant settlement ledger.
- Is a modular monolith so validation, liquidity checks, and central settlement remain in one ACID transaction.
- Never connects to a bank database.

### Simulation Engine

- Owns deterministic seeds, virtual-time state, normal-customer activity schedules, and safe infrastructure-failure controls.
- Uses normal public APIs with a simulation service identity; it never inserts financial rows directly.
- Keeps wall-clock leases and retries independent of virtual time.

### Operations Query

- Consumes sanitized versioned events into durable read models.
- Provides topology, transaction stages, ledger views, settlement snapshots, event exploration, and a resumable SSE stream.
- Is eventually consistent and cannot authorize, reject, settle, reverse, or correct money.

## Infrastructure Containers

- PostgreSQL may be a single local server but hosts isolated logical databases and roles.
- Kafka transports at-least-once domain events; owner outboxes and consumer inboxes provide reliability.
- Redis supports BFF sessions, rate limits, and caches only.
- OpenTelemetry Collector routes traces; Prometheus/Grafana, Tempo, and Loki provide metrics, traces, and logs.
- Optional schema-registry and load-test profiles may be added without becoming startup requirements for the core financial path.

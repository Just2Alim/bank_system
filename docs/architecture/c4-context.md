# C4 System Context

> Classification: **SIMULATOR_DESIGN_CHOICE**. Names and interactions in this diagram describe a fictional educational ecosystem, not a real Kazakhstan bank or payment operator.

## Context Diagram

```mermaid
flowchart LR
    Customer([Synthetic bank customer])
    BankOperator([Bank operator])
    NPCOperator([NPC Simulator operator])
    Auditor([Auditor / engineering reviewer])
    Developer([Developer / demonstrator])

    System["Kazakhstan Banking Ecosystem Simulator\nThree fictional banks, cards, payments, settlement, and operations"]
    Identity["OIDC Identity Provider\nKeycloak"]
    Observability["Observability Stack\nPrometheus, Grafana, Tempo, Loki"]
    FutureAF["Future Independent Antifraud System\nNOT IMPLEMENTED"]

    Customer -->|Views synthetic accounts; initiates transfers, MSMP and QR payments| System
    BankOperator -->|Operates a fictional bank and card processor| System
    NPCOperator -->|Inspects liquidity, RTGS queues and clearing cycles| System
    Auditor -->|Reads immutable journals, audit history and traces| System
    Developer -->|Seeds deterministic data, controls time, tests safe failures| System

    System <-->|OAuth 2.0 / OIDC| Identity
    System -->|Metrics, structured logs and traces| Observability
    System -.->|Versioned synthetic events in a future integration| FutureAF
```

## People and Responsibilities

| Person | Allowed use | Not allowed |
|---|---|---|
| Synthetic customer | Operate only accounts bound to the authenticated synthetic customer and bank | Select another bank/customer in a request body; use operational APIs |
| Bank operator | Inspect and operate resources belonging to one fictional bank | Modify another bank or NPP settlement records directly |
| NPC Simulator operator | Inspect participants, liquidity, queues, cycles, and safe failure controls | Edit bank customer balances or journals |
| Auditor | Read cross-system audit, ledger, message, and trace views | Execute financial commands |
| Developer/demonstrator | Seed/reset explicitly marked development data and run simulations | Invoke development reset controls in a production profile |

## System Boundary

Inside the simulator boundary are the three banks, their card processors, the national payment platform simulator, the simulation engine, the user interface, operational projections, Kafka, PostgreSQL, Redis, and local observability.

Keycloak is shown as a supporting external system because it has its own security lifecycle and database. It remains part of the local Compose deployment.

The future antifraud system is deliberately outside the implementation boundary. It may later subscribe to sanitized synthetic events, but the current platform contains no risk score, fraud flag, classifier, blocking rule, or analyst fraud workflow.

## Trust Boundaries

```mermaid
flowchart TB
    Browser[Untrusted browser]
    Gateway[Gateway / BFF trust boundary]
    Bank[Bank service identity boundary]
    Card[Card processor identity boundary]
    NPP[NPP participant boundary]
    OwnerDB[(Owner database)]
    Kafka[(Kafka with topic ACLs)]

    Browser -->|Secure cookie + CSRF token| Gateway
    Gateway -->|JWT with trusted bank/customer claims| Bank
    Bank <-->|Client credentials; idempotent commands| Card
    Bank <-->|Participant credentials; validated ISO subset| NPP
    Bank -->|Owner credential only| OwnerDB
    Card -->|Owner credential only| OwnerDB
    NPP -->|Owner credential only| OwnerDB
    Bank -->|Produce/consume permitted topics only| Kafka
    Card -->|Produce/consume permitted topics only| Kafka
    NPP -->|Produce/consume permitted topics only| Kafka
```

Browser-supplied `bankId`, actor, role, account owner, and participant identity are never trusted when those values can be derived from an authenticated principal or service credential.

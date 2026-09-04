# ADR 0003: Transactional Outbox and Idempotent Inbox

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE

## Context

Updating a ledger and publishing Kafka independently creates a dual-write gap. Kafka delivery is at least once, so every consumer can also see duplicates.

## Decision

Authoritative state, audit metadata, and an outbox row commit in the same PostgreSQL transaction. A publisher claims rows with bounded batches and `FOR UPDATE SKIP LOCKED`, publishes a versioned event envelope, and records publication state. Failed delivery retries with backoff and a visible dead-letter path.

Every consumer stores `eventId` in an inbox table under a unique constraint in the same transaction as its business/projection effect. Event partition keys preserve aggregate ordering; `aggregateVersion` detects gaps. Consumers must tolerate unknown additive fields.

Financial HTTP commands also use durable idempotency scoped by principal, bank, operation, and `Idempotency-Key`, with a canonical request hash. A repeated key/body replays the original receipt; a conflicting body returns `409`.

## Consequences

- There is no claim of exactly-once transport.
- Exactly-once business effect is achieved through database uniqueness and idempotent handlers.
- Operational screens expose lag, retry, and dead-letter state.

# ADR 0001: Bound Services by Financial Authority

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE

## Context

The simulator must show distributed banking behavior without splitting a single accounting transaction across arbitrary services. It must also prove that three banks, three card processors, and the national platform do not share authoritative financial state.

## Decision

Use a modular-monolith-per-financial-authority topology:

- one `bank-platform` artifact deployed for Orda, Nomad, and Tengri;
- one remote `legacy-core-simulator` that is Orda's sole customer-account and ledger authority;
- one `card-processing-simulator` artifact deployed once per bank with an isolated database;
- one modular `national-payment-platform` owning participants, aliases, ISO records, RTGS, clearing, and the central settlement ledger;
- separate `simulation-engine`, `operations-query`, `api-gateway`, and `web-console` deployables.

Local Compose may use one PostgreSQL server, but it creates separate logical databases and users. No service receives another authority's database credential. Cross-bank money moves only through national-platform APIs and events.

## Consequences

- Ledger postings that must balance share one database transaction.
- Distributed failure is visible only at real ownership boundaries.
- Deployment count remains realistic on a laptop.
- Isolation is testable with credentials and negative connectivity tests.

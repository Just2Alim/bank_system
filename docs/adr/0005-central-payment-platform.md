# ADR 0005: Central Payment Platform Owns Interbank Finality

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE informed by CONFIRMED NBK/NPCK concepts

## Context

NBK publicly describes an individual real-time gross-settlement rail and a net-basis small-value clearing rail. NPCK describes MSMP phone/QR addressing and ISO 20022 messaging. A simulator still needs one unambiguous authority for participant liquidity and settlement finality.

## Decision

`national-payment-platform` alone owns participant status, aliases, route selection, ISO-message records, RTGS queues, clearing cycles, and a balanced central participant-liability ledger.

- RTGS settles each accepted instruction immediately when liquidity is available; otherwise it queues FIFO and can settle, expire, or be cancelled before finality.
- Clearing accepts eligible items into one open cycle, freezes that cycle, computes zero-sum multilateral positions, and posts one atomic central journal.
- MSMP and simulated QR are addressing/orchestration modes, not alternative balance stores.
- After central settlement, a payment never regresses. Receiver outage causes durable delivery retry or a new linked return, never rollback of final settlement.

## Consequences

- Interbank flows are observable sagas rather than distributed database transactions.
- Bank and platform books can be reconciled independently.
- The implementation is conceptually faithful but explicitly not connected to or certified by Kazakhstan's production infrastructure.

# ADR 0004: Orda Uses a Remote Legacy-Core Authority

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE informed by public modernization signals

## Context

The brief requires materially different bank architectures. Public career material supports coexistence of legacy-core roles and modern digital technologies in Kazakhstan, but it does not expose any bank's full private architecture.

## Decision

Orda's `bank-platform` is a digital edge. It owns channel workflows and customer-facing saga status but delegates customers, accounts, holds, journals, statements, and balance reads through a narrow idempotent `CoreBankingPort` to `legacy-core-simulator`.

The edge database must contain no balance, hold, journal, or ledger-entry tables. It may keep short-lived non-authoritative display caches. Nomad embeds the native banking core for a digital-native profile. Tengri embeds the same accounting invariants behind a classic-core adapter boundary for a hybrid profile.

## Consequences

- Orda demonstrates latency, retry, circuit breaking, and uncertainty at a realistic adapter boundary.
- All banks retain identical accounting semantics while their implementation topology differs.
- The fictional names and design must not be presented as a copy of Halyk, Kaspi, BCC, Freedom, or ForteBank.

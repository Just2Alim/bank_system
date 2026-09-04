# ADR 0002: Immutable Double-Entry Ledgers

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE

## Context

Directly mutating an account balance cannot explain history, prove reconciliation, or represent reversals safely. Customer deposits are liabilities of each fictional bank, while the national platform maintains its own participant-liability ledger.

## Decision

Every monetary effect is a balanced journal containing positive `DEBIT` or `CREDIT` entries. A journal balances independently per currency and is immutable after posting. Corrections are new linked reversal, compensation, refund, or return journals. `NUMERIC(19,2)` and Java `BigDecimal` are mandatory; v1 movements are KZT-only and perform no hidden FX.

Book-balance projections update only inside the posting transaction and are reconstructable from entries. Active holds reduce available balance but create no general-ledger entry until capture.

Deterministic row locking protects concurrent debits. Database constraints/triggers reject unbalanced posting and mutation/deletion of posted entries.

## Consequences

- Statements, balances, and reconciliation have one auditable source.
- Replays and retries can prove a single monetary effect.
- Storage grows append-only and therefore requires retention/partition planning, not destructive cleanup.

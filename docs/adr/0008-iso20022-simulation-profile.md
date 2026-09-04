# ADR 0008: Pin a Replaceable ISO 20022 Simulation Profile

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE informed by CONFIRMED ISO/NPCK sources

## Context

Kazakhstan completed an ISO 20022 migration and NPCK publicly references pacs.008 for MSMP. Public sources do not establish every production variant, market-practice rule, or private participant mapping required for certification.

## Decision

Pin a documented v1 educational subset behind an `IsoMessageMapper` boundary:

- `pacs.008.001.08` credit-transfer concepts;
- `pacs.002.001.10` status concepts;
- `pacs.004.001.09` return concepts;
- query-generated `camt.053.001.08` statement concepts.

Every message stores its namespace/version, sender-scoped message ID, payload hash, business references, validation result, and linked simulator payment. XML input is UTF-8, at most 1 MiB, allowlisted by namespace, XSD-validated, and parsed with DTD, external entities, and XInclude disabled. Raw XML is redacted from ordinary logs.

## Consequences

- Examples are useful for learning and tracing but must say "not certified".
- Later research can add a new mapper/profile without changing ledger semantics.
- Duplicate message ID with the same hash replays status; a different hash is rejected as conflict.

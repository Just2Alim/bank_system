# ADR 0009: Antifraud Is an External Future Consumer

- Status: Accepted
- Date: 2026-09-04
- Classification: Product scope boundary

## Context

NBK publicly documents a National Antifraud Center, while this task explicitly excludes antifraud implementation. Mixing detection concepts into the banking simulator would bias generated data and blur ownership.

## Decision

The implemented system contains no fraud/risk score, model, label, rule engine, suspicious persona generator, case-management workflow, or automatic blocking decision. It produces only normal synthetic banking activity and versioned domain events useful to a future independent consumer.

The future consumer is shown outside the C4 system boundary. No current API depends on it. Schemas and source are scanned for prohibited scoring/label fields. Ordinary banking controls such as authentication, limits, account status, insufficient-funds checks, and operator-injected availability failures remain in scope and must not be described as fraud detection.

## Consequences

- Raw banking behavior remains unbiased for later experimentation.
- Security and financial controls are still fully enforced.
- Any later antifraud project requires a separate architecture decision and service boundary.

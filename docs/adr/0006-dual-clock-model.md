# ADR 0006: Separate Wall Clock and Simulation Clock

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE

## Context

Acceleration is useful for salary days, clearing cycles, expiry, and traffic generation. Applying an accelerated clock to tokens, audit, retry delays, or security expiry would make behavior unsafe and misleading.

## Decision

Use two explicit clocks:

- a monotonic/wall clock for authentication, audit timestamps, timeouts, retry backoff, rate limits, health, and trace duration;
- a controllable business clock for posting/value dates, scheduled activity, clearing windows, and simulated hold/card expiry.

The simulation engine owns business time, deterministic seed/profile, speed, and lifecycle revision. Services receive clock changes as versioned events and persist the simulation timestamp beside wall-clock event time. Allowed speeds are 1x, 10x, 60x, and 360x.

## Consequences

- Runs can be repeated from a seed without weakening security semantics.
- APIs/events must label both timestamps clearly.
- Tests inject both clocks and cannot call the system clock directly inside domain logic.

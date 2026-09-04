# Contract Conventions

## Identity and Authorization

The gateway derives the bank, customer, and operator role from the authenticated BFF session. A browser request cannot select an acting customer or bank in its JSON body. Internal APIs derive the calling participant or service from a short-lived service token. Resource-level authorization still applies after authentication.

Browser tokens remain in the gateway's server-side session store. The browser receives an opaque, HttpOnly, SameSite cookie. Every browser mutation also carries `X-CSRF-Token`; credentials are never placed in URLs or the SSE query string.

All contract examples use identifiers beginning with `SIM-`. Account, card, alias, and beneficiary identifiers are simulator tokens and are deliberately non-routable. Card interfaces never expose a payment account number, and alias interfaces never require a telephone-shaped value.

## Required Headers

| Header | Scope | Rule |
|---|---|---|
| `X-Correlation-ID` | Every operation | Caller supplies a synthetic correlation ID or the gateway creates one. The accepted ID is returned and propagated through commands, events, and traces. |
| `Idempotency-Key` | Every state-changing command except read-like alias/QR resolution | Scope is authenticated principal + operation + key. Store the canonical request hash and final/accepted response durably. |
| `X-CSRF-Token` | Every public gateway POST | Required in addition to the same-origin BFF session cookie. |
| `Last-Event-ID` | Optional SSE reconnect | Decimal operations sequence last applied by the client. |
| `traceparent` | HTTP propagation and event envelope | W3C trace context; correlation ID remains the business-flow identifier. |

Reusing an idempotency key with identical canonical content replays the recorded result and sets `Idempotent-Replay: true`. Reusing it with different content returns `409`. A timeout is an unknown outcome: query or retry the exact same command/message identifier.

## Money

Money is always:

```json
{
  "amount": "125000.00",
  "currency": "KZT"
}
```

`amount` is a non-negative decimal string with exactly two fractional digits. JSON floating-point amounts, exponent notation, implicit rounding, negative command amounts, and cross-currency posting are rejected. A direction or ledger side is modeled separately from amount.

KZT is the enabled end-to-end V1 currency. The shared shape permits another ISO currency code so later same-currency product APIs do not need a different representation; it does not authorize implicit conversion.

## Errors and Receipts

Errors use `ErrorV1` with a stable machine code, safe message, wall-clock UTC timestamp, trace ID, optional request ID, and structured safe details. Stack traces, credentials, infrastructure addresses, and unredacted external payloads are never returned.

State-changing asynchronous work returns `CommandReceiptV1`. `ACCEPTED` or `PROCESSING` is not proof of a debit or central settlement. The receipt links to the authoritative owner resource that callers query after a timeout.

## Time

Every domain event has wall-clock `occurredAt`; it may also have `simulationTime`. Wall time controls authentication, audit ordering, retries, leases, expiry of technical records, and hold recovery. Virtual time controls business schedules, value dates, activity generation, and clearing cut-offs only.

## Finality and Compensation

The NPP payment resource is authoritative for central rail state. Once `finalSettlement` is true, the payment is never rolled back because a receiver is unavailable. The receiver retries its local recognition and beneficiary credit; a business return is a new reverse-direction message with its own identifiers.

Before final settlement, an origin bank may refund captured customer funds only after it records a definitive NPP rejection, expiry, or cancellation reference. A network timeout alone cannot authorize a refund.

## HTTP and Event Versioning

- Breaking HTTP changes use a new path version.
- Additive optional response fields may remain in V1; consumers must tolerate unknown response properties only where their implementation policy explicitly allows it.
- Every event has an `eventType` and integer `schemaVersion`.
- Producers publish an outbox event at least once; consumers combine event-ID inbox deduplication with a business uniqueness key.
- Kafka partition keys are aggregate identifiers. Ordering across aggregates is never assumed.
- Event payloads and operations views contain sanitized simulator identifiers and the minimum fields needed by consumers.

The V1 event envelope is intentionally neutral: downstream analytical systems may consume it independently, but no analytical output participates in these authorization or settlement contracts.

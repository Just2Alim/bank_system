# V1 API and Event Contracts

> Classification: **SIMULATOR_DESIGN_CHOICE**. These interfaces serve fictional banks and a fictional national payment layer. They are not the private API or certified message profile of any real institution.

The contract source of truth is the versioned, machine-readable content under [`contracts/`](../../contracts/). OpenAPI is stored as JSON so the repository can validate it without a YAML dependency; JSON is a valid OpenAPI 3.1 representation.

## Contract Index

| Consumer boundary | Contract |
|---|---|
| Browser to same-origin gateway/BFF | [`gateway-public-v1.openapi.json`](../../contracts/openapi/gateway-public-v1.openapi.json) |
| Orda bank platform to remote core; semantic parity for native cores | [`core-internal-v1.openapi.json`](../../contracts/openapi/core-internal-v1.openapi.json) |
| Synthetic POS/ATM and bank adapter to an isolated card processor | [`card-processing-internal-v1.openapi.json`](../../contracts/openapi/card-processing-internal-v1.openapi.json) |
| Bank participants and restricted operators to the national payment simulator | [`npp-internal-v1.openapi.json`](../../contracts/openapi/npp-internal-v1.openapi.json) |
| Restricted operators to virtual clock and normal-activity generator | [`simulation-internal-v1.openapi.json`](../../contracts/openapi/simulation-internal-v1.openapi.json) |
| Gateway/BFF to durable operations projections and SSE | [`operations-internal-v1.openapi.json`](../../contracts/openapi/operations-internal-v1.openapi.json) |

Shared JSON Schemas define decimal money, the safe error envelope, command receipts, event envelopes, service health, the SSE data body, and the supported ISO-like message subset. Examples contain only visibly synthetic identifiers.

## Implementation Rule

Controllers, generated clients, producer serializers, consumer validators, and contract tests must bind to these V1 shapes. Domain owners may add private implementation fields in their databases, but an API-compatible change requires one of:

- an optional additive field accepted by existing consumers;
- a new event type or schema version for a changed event payload;
- a new `/v2` HTTP path for a breaking HTTP change.

Do not silently reinterpret an existing enum value or change settlement finality semantics under the same version.

## Validation

From the repository root:

```bash
python3 contracts/tools/validate_contracts.py
python3 -m unittest discover -s contracts/tests -p 'test_*.py'
```

The validator uses only the Python standard library. It parses every JSON contract, resolves every local `$ref`, checks the OpenAPI operation baseline, validates each manifest-bound example, and rejects prohibited sensitive/scoring field names and phone-shaped sample values.

See also:

- [Contract conventions](contract-conventions.md)
- [SSE replay and reset](sse.md)
- [ISO-like simulator messages](iso-like-messages.md)

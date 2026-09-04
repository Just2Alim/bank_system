# ISO-like Simulator Message Subset

The national payment simulator accepts a small JSON subset inspired by three ISO 20022 payment message families:

| Simulator schema | Purpose |
|---|---|
| [`pacs.008-simulator.v1.schema.json`](../../contracts/json-schema/payments/pacs.008-simulator.v1.schema.json) | Participant-to-participant customer credit instruction over RTGS, MSMP, or clearing |
| [`pacs.002-simulator.v1.schema.json`](../../contracts/json-schema/payments/pacs.002-simulator.v1.schema.json) | Acceptance, queue, settlement, rejection, and return status |
| [`pacs.004-simulator.v1.schema.json`](../../contracts/json-schema/payments/pacs.004-simulator.v1.schema.json) | New reverse-direction return after the original payment reached final settlement |

This is not ISO XML, not a complete ISO implementation, and not a Kazakhstan-certified market profile. The message family/version labels make educational mapping explicit while the JSON contract stays small enough to implement and test locally.

## Submission Rules

1. The authenticated service principal supplies the sender participant identity.
2. `debtorParticipantId` in a credit instruction must match that principal.
3. The NPP stores `(sender participant, businessMessageId, canonical payload hash)` before processing.
4. The same identifier and hash returns the recorded outcome; a different hash returns `409`.
5. All identifiers and account references use visibly synthetic `SIM-` tokens.
6. The NPP validates participant status, currency, amount scale, rail eligibility, message identity, and legal state transition before queueing or settlement.

## Status Mapping

| Simulator state | ISO-like status | Meaning |
|---|---|---|
| `ACCEPTED` | `ACTC` | Technical validation completed; no central settlement yet |
| `QUEUED_LIQUIDITY` | `PDNG` | RTGS/MSMP instruction awaits sufficient participant liquidity |
| `QUEUED_CLEARING` | `ACSP` | Instruction is accepted into an open clearing cycle |
| `SETTLED` | `ACSC` | Central participant ledger journal committed; final in this simulator |
| `REJECTED` | `RJCT` | Definitive non-settlement outcome |
| `RETURN_PENDING` | `ACSP` | A separate return instruction is progressing |
| `RETURNED` | `ACSC` | The separate return instruction reached final settlement |

The original payment remains `SETTLED` after a return. Implementations link the new return by `originalPaymentId`; they never rewrite the original journal or payment state to imply that settlement did not occur.

## Samples

- [RTGS credit instruction](../../contracts/examples/iso20022/pacs.008-rtgs.sample.json)
- [MSMP credit instruction resolved from a synthetic alias token](../../contracts/examples/iso20022/pacs.008-msmp.sample.json)
- [Final settlement status](../../contracts/examples/iso20022/pacs.002-settled.sample.json)
- [Post-settlement return](../../contracts/examples/iso20022/pacs.004-return.sample.json)

The examples intentionally omit personal identifiers and all real-world routable data.

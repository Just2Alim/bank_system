# Operations SSE Contract

`GET /api/v1/operations/stream` is a resumable projection feed, not a financial source of truth. The gateway proxies it on the same origin so native `EventSource` can use the authenticated session cookie. No token appears in the URL.

## Frame

```text
id: 42017
event: transaction-stage
data: {"sequence":42017,"eventType":"transaction-stage","occurredAt":"2026-09-04T09:15:01Z","correlationId":"SIM-CORR-DEMO-20260904-0001","payload":{"transactionId":"SIM-PAY-RTGS-20260904-0001","stage":"CENTRAL_SETTLEMENT","status":"COMPLETED"}}

```

The `id` and `data.sequence` values must match. The server sends UTF-8 frames in monotonically increasing operations sequence order. Supported event names are:

- `projection-updated` for refreshing a dashboard slice;
- `transaction-stage` for one correlated transaction step;
- `topology-health` for a service or dependency status change;
- `reset-required` when the requested sequence is older than retained data;
- `heartbeat` to keep an otherwise idle connection observable.

The JSON body follows [`operation-stream-event.v1.schema.json`](../../contracts/json-schema/common/operation-stream-event.v1.schema.json).

## Reconnect and Gap Recovery

The client applies a frame once, persists the highest applied sequence in memory, and reconnects with `Last-Event-ID`. Duplicate IDs are ignored. A forward gap is not filled by guessing; the client reconnects or reloads the affected REST snapshot.

If replay is unavailable, the server emits:

```text
id: 55000
event: reset-required
data: {"sequence":55000,"eventType":"reset-required","occurredAt":"2026-09-04T09:20:00Z","correlationId":null,"payload":{"reason":"RETENTION_GAP","snapshotBaseSequence":55000}}

```

The client then fetches the relevant REST snapshots, records their `metadata.sequence`, discards older buffered frames, and resumes after that sequence.

## Operational Requirements

- Return `Content-Type: text/event-stream`, `Cache-Control: no-cache`, and disable reverse-proxy buffering.
- Authorize the operator before opening the stream and end the stream when the session expires.
- Keep heartbeat cadence on wall-clock time, even while virtual time is paused.
- Apply bounded per-client buffering. Disconnect a slow client instead of blocking projection ingestion.
- Redact data before persistence in the operations read model; the SSE layer does not perform last-minute financial-data discovery.

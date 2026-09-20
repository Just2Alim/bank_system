# BCC Open Banking Console Specification

## Goal

Provide a runnable web console backed by a server-side provider boundary. The default provider supplies deterministic demo data. The BCC provider uses the Bank CenterCredit Financial API sandbox for accounts and statements after credentials are configured.

## Safety boundary

- Browser code never receives `client_secret` or an OAuth access token.
- Only sandbox endpoints are enabled by the supplied configuration.
- The BCC provider is read-only in this increment. Payment creation remains a local demo command until the exact subscribed BCC payment schema and signing/approval flow are exported from the user's developer application.
- External responses are normalized into the existing `/api/v1` console contract.
- Secrets are read only from environment variables and are excluded from Git.

## User flows

1. Open the console and see whether the data source is `DEMO` or `BCC_SANDBOX`.
2. View accounts and recent transactions.
3. Create an idempotent demo transfer and immediately see its effect in accounts and activity.
4. Configure BCC sandbox credentials and reload to read sandbox accounts and statements.

## Configuration

| Variable | Purpose |
| --- | --- |
| `OPEN_BANKING_PROVIDER` | `demo` or `bcc` |
| `BCC_CLIENT_ID` | OAuth client id |
| `BCC_CLIENT_SECRET` | OAuth client secret |
| `BCC_APP_ID` | Application id used in Financial API paths |
| `BCC_TOKEN_URL` | OAuth endpoint; defaults to BCC sandbox |
| `BCC_FINANCIAL_BASE_URL` | Financial API base URL; defaults to BCC sandbox |

## Acceptance criteria

- Gateway tests prove response envelopes, idempotent demo transfers, balance conservation, OAuth token handling, and BCC response normalization.
- Console tests prove source status rendering and transfer submission.
- Maven tests and frontend test/build commands pass.
- The Docker topology starts the gateway and web console without embedding credentials.

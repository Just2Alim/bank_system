# BCC Open Banking Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a secure BCC sandbox adapter and a working interactive web console over one stable gateway API.

**Architecture:** A new Spring Boot gateway owns OAuth credentials and translates provider-specific data into the existing console contract. A deterministic in-memory provider implements the same port for immediate use and safe mutation demos; the BCC provider is read-only until the subscribed payment contract is supplied.

**Tech Stack:** Java 21, Spring Boot 3.5, RestClient, JUnit 5, React 19, TypeScript, TanStack Query, MUI, Vitest, Docker Compose.

**Spec:** `docs/specs/bcc-open-banking-console.md`

## Global Constraints

- Never expose BCC credentials or OAuth tokens to the browser or Git.
- Default to BCC sandbox endpoints, never production.
- Preserve immutable financial records and idempotent commands.
- Keep external-bank DTOs outside the simulator domain model.
- BCC writes remain disabled until the exact subscribed payment contract and approval flow are verified.

## Review Focus

- Missing credentials must fail at startup only when `OPEN_BANKING_PROVIDER=bcc`.
- Repeating one idempotency key must not post a transfer twice.
- A demo transfer must conserve total balances.
- Unknown BCC JSON fields must not break normalization.
- Remote errors must return a safe API error without leaking credentials or tokens.

---

### Task 1: Gateway provider boundary

**Files:**
- Create: `services/open-banking-gateway/**`
- Modify: `services/pom.xml`
- Test: `services/open-banking-gateway/src/test/**`

**Interfaces:**
- Produces: `OpenBankingProvider.accounts()`, `transactions(int)`, `transfer(TransferCommand)`.

- [ ] Write failing tests for deterministic accounts, balance conservation, and idempotency.
- [ ] Run the focused tests and confirm failure because the provider is absent.
- [ ] Implement the provider records and demo provider.
- [ ] Run focused and module tests.

### Task 2: BCC OAuth and Financial API adapter

**Files:**
- Create: `services/open-banking-gateway/src/main/java/kz/sim/bank/gateway/bcc/**`
- Test: `services/open-banking-gateway/src/test/java/kz/sim/bank/gateway/bcc/**`

**Interfaces:**
- Consumes: BCC OAuth token and Financial API JSON.
- Produces: normalized `AccountView` and `TransactionView` values.

- [ ] Write failing HTTP-stub tests for Basic-auth token acquisition and bearer-auth account retrieval.
- [ ] Verify the tests fail for the missing adapter.
- [ ] Implement cached OAuth acquisition and tolerant JSON normalization.
- [ ] Run focused and module tests.

### Task 3: Console API and interactive UI

**Files:**
- Create: gateway REST controllers and configuration.
- Modify: `apps/web-console/src/lib/schemas.ts`, routes, shell, account and transfer pages.
- Test: controller and React page tests.

**Interfaces:**
- Produces: `/api/v1/integration/status`, `/api/v1/me/accounts`, `/api/v1/me/transactions`, `/api/v1/transfers`.

- [ ] Write failing controller and UI tests for status, account rendering, and transfer submission.
- [ ] Implement envelope endpoints and the console workflow.
- [ ] Run backend and frontend suites.

### Task 4: Runnable topology and operator documentation

**Files:**
- Modify: `infrastructure/docker/compose.yaml`, `infrastructure/docker/README.md`, `.gitignore`.
- Create: `.env.example`, gateway/web Dockerfiles if required.

**Interfaces:**
- Produces: console on port `5173` and gateway on port `8080`.

- [ ] Add environment-only BCC configuration and demo defaults.
- [ ] Validate Compose configuration without secrets.
- [ ] Run Maven tests/package and frontend lint/typecheck/tests/build.
- [ ] Inspect `git diff` for secrets and unrelated user changes.

# ADR 0007: Same-Origin BFF with Server-Side OIDC Tokens

- Status: Accepted
- Date: 2026-09-04
- Classification: SIMULATOR_DESIGN_CHOICE

## Context

The browser needs customer and operator workspaces plus resumable SSE. Keeping bearer tokens in browser storage increases exposure, while native `EventSource` cannot conveniently attach authorization headers.

## Decision

The gateway acts as a same-origin backend-for-frontend. It performs OIDC Authorization Code with PKCE against Keycloak, stores tokens server-side in Redis, and gives the browser a Secure, HttpOnly, SameSite session cookie. Browser mutations require a bound CSRF token. Native EventSource uses the same-origin cookie and never puts a token in a query string.

Services independently validate internal JWT audience and role claims. Customer resources are subject-scoped; operations endpoints require operator/auditor roles; simulation mutations require operator/admin roles. CORS is deny-by-default outside configured development origins. Rate limits apply at the gateway and sensitive service boundaries.

## Consequences

- Token refresh and logout are centralized.
- Redis availability affects sessions but never financial authority.
- Security tests must cover CSRF, horizontal access, expired tokens, audience mismatch, role escalation, and log redaction.

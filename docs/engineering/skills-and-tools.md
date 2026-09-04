# Skills And Tools Inventory

Date: 2026-09-04

At the start of the implementation the repository contained only `README.md`. This document records the capability audit performed before scaffolding began; later repository files are implementation results, not evidence available at audit time.

## Repository Guidance In Scope

The user-provided `AGENTS.md` instructions are the active project guidance for this task. The most relevant constraints are:

- Plan complex work before writing code.
- Use test-driven development for new features and bug fixes.
- Target at least 80% coverage across unit, integration, and E2E tests.
- Validate inputs at system boundaries, avoid hardcoded secrets, and review security-sensitive code.
- Prefer small focused files, clear error handling, and immutable data updates.
- Treat networked tools as read-only unless the user explicitly approves external changes such as publishing, pushing, posting, merging, or changing credentials.

No checked-in `AGENTS.md`, `.codex/config.toml`, `.codex/agents/*.toml`, or `.agents/skills/*/SKILL.md` files exist in this repo at the time of inspection. The pasted AGENTS text mentions project-local roles such as `planner`, `tdd-guide`, `code-reviewer`, `security-reviewer`, and language reviewers, but no executable project-local agent role files were present in this checkout.

## Tools Actually Used For This Inventory

- `rg --files` to discover repository files.
- `find` to check for hidden Codex/ECC files not returned by the normal repository file scan.
- `ls -la`, `sed`, and `git status --short` to verify repo shape, README content, and working tree state.
- Local version probes for `java`, `javac`, `node`, `npm`, `pnpm`, `bun`, `docker`, `docker compose`, `git`, `mvn`, `gradle`, `psql`, and `kubectl`.
- Web search against official or reputable sources for Spring Boot, Spring Security, DDD, PostgreSQL, Kafka, Docker Compose, Kubernetes, Testcontainers, React Flow, OpenTelemetry, Micrometer/Actuator, OWASP, and Playwright.

No remote scripts were executed, no plugins were installed, and no external resources were modified.

## Local Runtime Snapshot

| Capability | Current State |
|---|---|
| Git | `git version 2.52.0` |
| Default Java runtime | Oracle Java `1.8.0_501` |
| Project Java runtime/compiler | Homebrew OpenJDK `26.0.1` at `/opt/homebrew/opt/openjdk`; compiles with `--release 21` |
| Maven | Not on `PATH`; verified Apache Maven `3.9.16` staged temporarily to generate the checked-in wrapper |
| Gradle | Not installed or not on `PATH` |
| Node.js | `v24.13.0` |
| npm | `11.12.1` |
| pnpm | `11.19.0` |
| Yarn | Not installed or not on `PATH` |
| Bun | `1.3.14` |
| Docker | `29.2.1` |
| Docker Compose | `v5.0.2` |
| Docker daemon | Not running during the initial audit; Compose integration must be rechecked after Docker Desktop starts |
| PostgreSQL CLI | `psql` not installed or not on `PATH` |
| kubectl | Client `v1.34.1`, kustomize `v5.7.1` |

Implication: the default Java 8 executable must not be used for this project. Builds set `JAVA_HOME=/opt/homebrew/opt/openjdk`, use a checked-in Maven Wrapper, and target Java 21 bytecode through `maven.compiler.release=21`. Java 21 remains the declared production baseline even though the local compiler is newer.

## Installed Codex Skills Discovered

System skills:

- `imagegen`
- `openai-docs`
- `plugin-creator`
- `skill-creator`
- `skill-installer`
- `review-agent`

Bundled/productivity skills:

- `browser:control-in-app-browser`
- `chrome:control-chrome`
- `computer-use:computer-use`
- `sites:sites-building`
- `sites:sites-hosting`
- `visualize`

Primary runtime skills:

- `documents`
- `pdf`
- `presentations`
- `spreadsheets`
- `spreadsheets:excel-live-control`
- `template-creator`

Curated remote/plugin skills present in the local cache:

- Canva skills for brand checks, branded presentations, bulk create, design editing, feedback implementation, resizing, translation, and design feedback.
- Media production skills for ad multiplier, brand asset creation, faceless video, narrator, product photoshoot, subtitles, thumbnail generation, UGC product/review/tutorial/try-on/unboxing/website video.
- `plugin-management`
- `deep-research-work:deep-research`
- OpenAI artifact template skills such as strategy memorandum, system design, analytics dashboard, financial budget, project tracker, operating review, and related templates.

These skills are installed or cached for this Codex environment, but they are not project-local workflow dependencies. For this backend-first bank system, the directly useful built-ins are mostly `openai-docs` for OpenAI/Codex questions, `browser`/`chrome` for interactive local web verification, `visualize` for architecture diagrams or exploration tools, and the document/spreadsheet/pdf skills for generated artifacts.

## Candidate Engineering Workflows And Sources

### Spring Boot And Security

Use Spring Boot 3.5.x as the primary Java application framework with Java 21 as requested. Spring Boot's official documentation covers Testcontainers integration for integration tests against real services, and Spring Boot Actuator supports health endpoints and Kubernetes probe integration.

Spring Security is the default security candidate for authentication, authorization, and common web exploit protection in Spring applications. Its reference documentation covers CSRF handling for servlet applications and SPA integration.

Sources:

- Spring Boot Testcontainers documentation: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Spring Boot Actuator endpoints and probes: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Security reference: https://docs.spring.io/spring-security/reference/index.html
- Spring Security CSRF reference: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Boot repository license: https://github.com/spring-projects/spring-boot

License/reputation: Spring Boot is an official Spring project with Apache 2.0 licensing. Spring Security is the canonical Spring security framework.

Decision: use official Spring docs and ECC security/TDD guidance first. Add Spring dependencies only when application scaffolding begins.

### Domain-Driven Design

DDD is relevant for a bank system because account, customer, transfer, ledger, risk, compliance, and notification concepts should not collapse into one ambiguous model. Microsoft Azure Architecture guidance and Martin Fowler's bounded context write-up both emphasize dividing large domains into bounded contexts with explicit models and relationships.

Sources:

- Microsoft Azure Architecture, tactical DDD: https://learn.microsoft.com/en-us/azure/architecture/microservices/model/tactical-domain-driven-design
- Martin Fowler, bounded context: https://martinfowler.com/bliki/BoundedContext.html

License/reputation: Microsoft Learn and Martin Fowler are reputable architecture sources. These are workflow references, not code dependencies.

Decision: use DDD as a modeling practice during planning. Do not add a DDD framework dependency unless a concrete implementation need appears.

### PostgreSQL

PostgreSQL is the preferred relational database candidate for a bank system because transactional consistency, constraints, indexes, query planning, and operational visibility matter. Official docs cover indexes, `EXPLAIN`, and cumulative statistics.

Sources:

- PostgreSQL docs: https://www.postgresql.org/docs/
- PostgreSQL indexes: https://www.postgresql.org/docs/current/indexes.html
- PostgreSQL `EXPLAIN`: https://www.postgresql.org/docs/current/using-explain.html
- PostgreSQL cumulative statistics: https://www.postgresql.org/docs/current/monitoring-stats.html
- PostgreSQL license: https://www.postgresql.org/about/licence/

License/reputation: PostgreSQL is released under the permissive PostgreSQL License by the PostgreSQL Global Development Group.

Decision: choose PostgreSQL over embedded-only stores for realistic integration testing and production parity. Use migrations and Testcontainers when the service exists.

### Kafka And Eventing

Kafka is a required transport for versioned banking-domain events, transactional-outbox publication, operations projections, notifications, and future independent consumers. Financial correctness still belongs to each service's database transaction and immutable ledger; Kafka delivery is at least once and consumers deduplicate through inbox records.

Sources:

- Apache Kafka documentation: https://kafka.apache.org/documentation/
- Apache Kafka quickstart: https://kafka.apache.org/quickstart/
- Apache Kafka Streams docs: https://kafka.apache.org/documentation/streams/
- Apache Kafka GitHub/license notes: https://github.com/apache/kafka

License/reputation: Apache Kafka is an Apache Software Foundation project under Apache 2.0.

Decision: use pinned Apache Kafka in KRaft mode. Keep topics coarse-grained and version contracts explicitly; never treat Kafka as the source of truth for balances or settlement positions.

### Docker, Compose, And Kubernetes

Docker and Compose are already installed locally and are useful for repeatable dev environments. Kubernetes tooling is available through `kubectl`, but no cluster was verified. Kubernetes should be treated as a deployment target, not a local development requirement unless the project needs it.

Sources:

- Docker Compose docs: https://docs.docker.com/compose/
- Docker Compose repository/license: https://github.com/docker/compose
- Kubernetes probes docs: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
- Spring Boot Actuator endpoints and probes: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html

License/reputation: Docker Compose is Apache 2.0. Kubernetes is a CNCF project under Apache 2.0.

Decision: use Docker Compose as the local runnable baseline and provide a Helm chart after the service topology and probes are stable, as required by the brief.

### Testcontainers

Testcontainers is the most useful external testing candidate discovered. It supports integration tests that start throwaway Docker services, which fits PostgreSQL and Kafka validation better than mocks or in-memory substitutes.

Sources:

- Spring Boot Testcontainers integration: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Testcontainers Spring Boot REST guide: https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/
- Testcontainers guides index: https://testcontainers.com/guides/
- Testcontainers Java repository/license: https://github.com/testcontainers/testcontainers-java

License/reputation: Testcontainers Java is MIT licensed and widely used for JVM integration testing.

Decision: add Testcontainers when a Java test suite is scaffolded. It is a justified dependency for integration tests, especially with PostgreSQL and Kafka.

### React, TypeScript, React Flow, And Playwright

React/TypeScript is relevant only if the bank system includes an administrative UI, transaction visualization, workflow designer, or operations console. React Flow is appropriate for node-based interfaces such as approval workflows, process maps, or transaction-flow exploration; it is not needed for ordinary CRUD screens.

Playwright is the strongest E2E test candidate for frontend or full-stack browser flows. It supports Chromium, Firefox, and WebKit automation and has a mature test runner.

Sources:

- React Flow docs: https://reactflow.dev/
- React Flow TypeScript guide: https://reactflow.dev/learn/advanced-use/typescript
- React Flow GitHub/license: https://github.com/xyflow/xyflow
- Playwright docs: https://playwright.dev/
- Playwright GitHub/license: https://github.com/microsoft/playwright

License/reputation: React Flow is MIT licensed. Playwright is maintained by Microsoft and is Apache 2.0 licensed.

Decision: the requested operations console justifies Playwright and React Flow: React Flow is limited to live topology/trace relationships, while ordinary screens use accessible tables and charts.

### Observability

Spring Boot Actuator and Micrometer should be the first observability layer for a Spring service. OpenTelemetry is the vendor-neutral candidate for traces, metrics, and logs once there is a deployment or multi-service boundary.

Sources:

- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring blog on OpenTelemetry with Spring Boot: https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot
- OpenTelemetry Spring Boot starter docs: https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/getting-started/
- OpenTelemetry Spring Boot starter instrumentation: https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/out-of-the-box-instrumentation/

License/reputation: OpenTelemetry is a CNCF project. Micrometer and Actuator are part of the Spring ecosystem.

Decision: start with Actuator/Micrometer health and metrics. Add OpenTelemetry when there is something meaningful to trace across process, queue, or service boundaries.

### Security Workflow

Use OWASP guidance as the external security baseline, alongside Spring Security references and the user-provided ECC security checklist. The most relevant early practices are input validation, SQL injection prevention through parameterized queries/ORM binding, CSRF configuration for browser sessions, XSS-safe rendering, authentication and authorization tests, and avoiding sensitive data in errors.

Sources:

- OWASP CSRF Prevention Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
- OWASP Cheat Sheet Series: https://cheatsheetseries.owasp.org/
- Spring Security reference: https://docs.spring.io/spring-security/reference/index.html

License/reputation: OWASP is a well-known application security nonprofit. These are process references, not runtime dependencies.

Decision: use OWASP and Spring Security docs as review checklists. Do not add security scanning dependencies until the build system exists; then prefer ecosystem-native checks such as Maven/Gradle dependency auditing and container/image scanning.

## Recommended Near-Term Path

1. Use the installed OpenJDK compiler with Java 21 release targeting and generate a checksum-pinned Maven Wrapper.
2. Scaffold the backend with Spring Boot 3.5.x, Spring Security, validation, Actuator, PostgreSQL, Flyway, and a TDD-ready test stack.
3. Add Testcontainers for PostgreSQL-backed integration tests as soon as persistence exists.
4. Add Docker Compose for local infrastructure once concrete service dependencies are known.
5. Add Kafka because event transport, projections, and live transaction tracing are explicit requirements.
6. Build the confirmed React/TypeScript customer and operations workspaces; use React Flow only for topology and transaction routes.
7. Add Playwright for the required browser-visible critical journeys.
8. Add OpenTelemetry after the service has deployment topology or cross-boundary calls worth tracing.

## Why Not Add More Now

The available Codex/ECC guidance already covers planning, TDD, review, security, frontend patterns, backend patterns, E2E testing, and verification loops. Additional plugins were therefore unnecessary. Runtime dependencies are admitted only when they serve an explicit requirement, are pinned, and can be checked against primary documentation. Testcontainers is justified for realistic PostgreSQL, Kafka, and Redis integration tests; it does not replace the final clean Compose verification.

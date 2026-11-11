# AI Incident Intelligence Platform

A GitHub-ready, **read-only incident investigation application**: Java 21 / Spring Boot API, React dashboard, LangGraph investigation workflow, PostgreSQL knowledge retrieval, and Docker Compose.

## What works

- Create, list, and retrieve incidents through REST APIs, persisted in PostgreSQL.
- Launch a LangGraph investigation from the dashboard; triage signals and explicit hypotheses are derived before retrieval.
- Track incident lifecycle (OPEN, INVESTIGATING, MITIGATED, RESOLVED).
- Persist investigation results for later review via `GET /api/incidents/{id}/investigations`.
- Retrieve relevant runbooks from PostgreSQL using full-text search (lexical RAG).
- Optionally generate an evidence-grounded LLM assessment using OpenAI; without a key, use a deterministic rules-based assessment.
- Display findings, retrieved runbooks, and human-reviewed next steps.
- No remediation is executed automatically.

> **Scope:** This is a functional starter, not a production incident-response system. PostgreSQL includes the pgvector extension and an optional embedding column, but **vector embeddings / semantic similarity search are not yet implemented**. There are no Kafka consumers, external log integrations, real-time anomaly detectors, or auth/RBAC in this starter.

## Architecture

```text
React :3000 -> nginx /api proxy -> Spring Boot :8080 -> PostgreSQL :5432
                                            |
                                            +-> FastAPI :8000 -> LangGraph
                                                                   |
                                                           PostgreSQL runbooks
                                                                   |
                                                         Optional OpenAI LLM
```

## Quick start

Requires Docker Compose. From the repository root:

```bash
cp .env.example .env
# Optional: set OPENAI_API_KEY in .env for LLM responses
docker compose up --build -d
```

Open **http://localhost:3000**. Create an incident, then click **Investigate**. First build downloads Docker images, Maven artifacts, and npm/Python dependencies.

API health: `http://localhost:8080/actuator/health` and agent health: `http://localhost:8000/health`.

Example request:

```bash
curl -X POST http://localhost:8080/api/incidents \
  -H 'Content-Type: application/json' \
  -d '{"title":"Kafka consumer lag","description":"Lag increased after deployment","serviceName":"orders-service","severity":"HIGH"}'
curl -X POST http://localhost:8080/api/incidents/1/investigate
```

## Local tests

```bash
cd backend && mvn test
cd ../agent && pip install -r requirements.txt && pytest -q
cd ../frontend && npm install && npm run build
```

The agent unit tests do not require PostgreSQL; the full `/investigate` endpoint does.

## API endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/incidents` | List incidents |
| POST | `/api/incidents` | Create incident |
| GET | `/api/incidents/{id}` | Retrieve incident |
| PATCH | `/api/incidents/{id}/status` | Update lifecycle status |
| GET | `/api/incidents/{id}/investigations` | Investigation history |
| POST | `/api/incidents/{id}/investigate` | Run investigation |
| GET | `/actuator/health` | API health |
| GET | `:8000/health` | Agent health |

## Security and operational limitations

- Demo credentials are intentionally local-only. Never deploy with the default password.
- Endpoints are unauthenticated; do not expose the stack publicly.
- Incident text is sent to the configured LLM provider if a key is present. Do not submit secrets, PII, or real production logs without a privacy review.
- LLM output is advisory and may be wrong. Investigations do not perform production changes.
- Schema is bootstrapped by Hibernate and SQL; use Flyway or Liquibase for production migrations.
- The synchronous investigation API can time out on slow LLM calls; production systems should use queued jobs, persistence, retries, and rate limits.

## Suggested next milestones

1. Add pgvector semantic retrieval and embedding ingestion, with retrieval evaluations.
2. Add Kafka incident events, idempotent processing, retry/DLQ and an outbox.
3. Integrate OpenTelemetry, Prometheus, Grafana, Loki/Splunk, and deployment metadata.
4. Add OAuth2/OIDC, RBAC, audit logs, rate limits, secret management and human approvals.
5. Add Langfuse tracing, LLM quality evaluations, and incident lifecycle workflows.

## License

MIT. See LICENSE.

## Walkthrough: a realistic demo

1. Create an incident: `Kafka consumer lag after deployment`, service `orders-service`, severity `HIGH`.
2. Click **Investigate**. The agent identifies text-based signals, proposes hypotheses, retrieves runbook evidence, and returns a cautious assessment.
3. Review recommended actions with an operator; the system **does not** change production resources.
4. Mark the incident `MITIGATED` and later `RESOLVED` in the dashboard.
5. Query `/api/incidents/1/investigations` to see stored investigation records.

## Engineering decisions

- **Separation of concerns:** Spring Boot owns incident lifecycle and persistence; Python owns AI workflow; React owns the user experience.
- **Explainability:** signals, hypotheses, retrieved references, and actions are returned separately.
- **Safe default:** no LLM key is required; the deterministic fallback makes the demo usable offline (once containers and dependencies are installed).
- **No invented observability:** signals are keywords in incident text, not actual Prometheus or Splunk measurements.
- **Deliberate tradeoff:** investigations are synchronous for simplicity; a production system needs queued background jobs and idempotency.

## Roadmap (not implemented)

Kafka event ingestion, pgvector semantic embeddings, real telemetry collectors, Langfuse traces, authentication, role-based approvals, deployment automation, and SLO-based alerting.

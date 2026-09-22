# ZipRun AI Reassignment Engine

Reactive delivery-agent reassignment engine built with Spring Boot and React.

## Run locally

Requirements: Java 21, Maven, Node.js 20+, and npm.

Start the backend from the repository root:

```bash
ROUTING_STRATEGY=ai mvn spring-boot:run
```

The backend runs at `http://localhost:8080`. It uses H2 in-memory storage and seeds five agents and eight orders on startup. The default LLM provider is a deterministic local stub, so no API key is required.

Start the frontend in a second terminal:

```bash
cd hackathon-ui
npm install
npm run dev
```

Open `http://localhost:5173`.

## Demo path

1. Confirm the agent roster and seeded orders appear.
2. Set an assigned agent, such as `Priya Sharma`, to `Offline`.
3. Wait for polling or press `Refresh now`.
4. Review the `Re-plan / offline event` suggestions and AI reasoning.
5. Accept one suggestion and reject another.
6. Confirm the queue changes and the accepted order becomes `REASSIGNED`.

## Providers

The default `stub` provider is ideal for repeatable demos. For a real provider, configure environment variables without committing secrets:

```bash
ROUTING_STRATEGY=ai \
LLM_PROVIDER=gemini \
LLM_API_KEY="$LLM_API_KEY" \
LLM_MODEL=gemini-1.5-flash \
LLM_BASE_URL=https://generativelanguage.googleapis.com \
mvn spring-boot:run
```

`groq` and `ollama` use OpenAI-compatible request formats. Provider errors, malformed JSON, invalid agent IDs, and invalid confidence values fall back to rule-based routing.

## API quick reference

- `GET /agents`
- `POST /orders`
- `GET /orders?status=ASSIGNED`
- `PATCH /agents/{id}/status`
- `POST /orders/{id}/suggest`
- `GET /suggestions?status=PENDING`
- `PATCH /suggestions/{id}` with `{"status":"ACCEPTED"}` or `{"status":"REJECTED"}`
- `GET /routing/strategy`
- `PATCH /routing/strategy` with `{"strategy":"ai"}` or `{"strategy":"rule-based"}`
- `GET /actuator/health`

## Verification

Backend tests:

```bash
mvn test
```

Frontend checks:

```bash
cd hackathon-ui
npm run build
npm run lint
```

The tests cover lowest-load routing, tie-breaking, valid AI JSON, hallucinated-agent fallback, runtime strategy switching, idempotent offline replanning, and suggestion acceptance.

## Evaluation alignment

The implementation is organized around the brief's scoring areas: domain/API, pluggable routing, AI integration and resilience, asynchronous agentic loop, ops UI floor, and ADR/walkthrough. See [ADR.md](ADR.md) for the decisions and tradeoffs a reviewer should inspect.

# AI Reassignment Engine: Hackathon Plan

## 1. Objective

Build a reactive reassignment engine for ZipRun delivery operations.

When an agent becomes `OFFLINE`, the system must:

1. Detect the status change.
2. Find orders assigned to that agent.
3. Run the configured routing strategy for each affected order.
4. Persist a reassignment suggestion with confidence, reasoning, and trigger metadata.
5. Show the suggestion to ops for approval.
6. Let ops accept or reject it without auto-assigning the order.

The core demo path is:

`PATCH agent OFFLINE -> async re-plan -> pending suggestion appears -> ops accepts/rejects`

## 2. Current workspace baseline

- Backend: Spring Boot project at the repository root.
- Main package: `com.zycus.hackthon`.
- Database dependency: H2 and Spring Data JPA are already present.
- Validation and Actuator dependencies are already present.
- Frontend: Vite React project in `hackathon-ui/`.
- Current frontend is still the Vite starter screen.
- No domain, controller, routing, LLM, or agentic-loop code exists yet.
- Current scaffold uses Java 21, Spring Boot 4.1.1, and React 19.

### Version decision

The brief names Spring Boot 3.x and React 18, while the supplied scaffold uses newer versions. Do not spend hackathon time downgrading unless the build or a required API fails. First run the backend and frontend builds, keep the existing versions if they work, and record the deviation in `ADR.md`. The implementation should avoid version-specific APIs where possible.

## 3. Scope and priorities

### Must ship

1. Domain model and persistence.
2. Required REST endpoints with validation and useful error responses.
3. Pluggable routing contract.
4. Deterministic rule-based routing strategy.
5. AI strategy with structured output validation and fallback.
6. Non-blocking, idempotent agent-offline re-planning.
7. Functional React ops screen.
8. `ADR.md`, README setup instructions, seed data, and demo flow.

### Defer first

- SSE token streaming.
- Full dispatch board.
- SLA countdowns and zone visualisation.
- Docker/deployment.
- Complex authentication or production infrastructure.

Protect correctness of the re-planning loop and fallback behavior before adding visual polish.

## 4. Proposed architecture

```text
React ops UI
    |
REST controllers
    |
Application services
    |-- OrderService
    |-- AgentService
    |-- SuggestionService
    |-- ReplanningService
    |
RoutingStrategy contract
    |-- RuleBasedRoutingStrategy
    |-- AiRoutingStrategy
    |
Repositories -> H2
    |
ApplicationEventPublisher -> async AgentOfflineListener
```

### Runtime strategy selection

Use a Spring-injected map of routing strategies keyed by a stable name such as `rule-based` and `ai`. A selector reads `routing.strategy` from configuration for every call. This keeps HTTP and async callers on the same contract and makes a future `ZoneAffinityStrategy` additive.

For the short session, “runtime switchable” means configuration/environment-driven selection without code changes. If true live switching without restart is required, add a small admin/config endpoint only after the core flow works.

### Async event flow

The agent status service publishes an `AgentOfflineEvent` after changing an agent to `OFFLINE`. An async listener loads affected orders and creates suggestions. The PATCH endpoint returns without waiting for the LLM or re-planning work.

Use a bounded Spring executor if available. Catch and log failures per order so one failed recommendation does not stop the remaining orders.

## 5. Domain model

### `Agent`

- `id`: stable identifier such as `AGT-001`.
- `name`.
- `status`: `AVAILABLE`, `BUSY`, `OFFLINE`.
- `activeOrderCount` or a derived count from orders.
- Future extension points: nullable `currentZone`, `maxCapacity`.

### `Order`

- `id`: stable identifier such as `ORD-001`.
- `description`.
- `assignedAgent`.
- `status`: `ASSIGNED`, `REASSIGNMENT_PENDING`, `REASSIGNED`, `DELIVERED`.
- `createdAt`.
- Future extension points: nullable `pickupZone`, `dropoffZone`, `weightClass`, `slaDeadline`.

### `ReassignmentSuggestion`

- `id`.
- `order`.
- `recommendedAgent`.
- `confidence`: decimal from `0.0` to `1.0`.
- `reasoning`.
- `status`: `PENDING`, `ACCEPTED`, `REJECTED`.
- `triggerReason`: `INITIAL`, `AGENT_OFFLINE`.
- `createdAt`.

Add a repository query for pending offline suggestions by order. This is the idempotency guard when an agent is toggled offline more than once.

## 6. API contract

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/orders` | Create an order pre-assigned to an agent |
| `GET` | `/orders?status=` | List orders, optionally filtered by status |
| `PATCH` | `/agents/{id}/status` | Change agent status; `OFFLINE` publishes the event |
| `GET` | `/agents` | List agents and statuses for the UI |
| `POST` | `/orders/{id}/suggest` | Run the active strategy and persist a suggestion |
| `GET` | `/suggestions?status=PENDING` | Load pending suggestions for polling |
| `PATCH` | `/suggestions/{id}` | Accept or reject a pending suggestion |
| `GET` | `/actuator/health` | Verify the application is running |

Use request DTOs rather than exposing entities directly. Return `400` for invalid input, `404` for missing resources, `409` for invalid state transitions, and `200`/`201` for successful reads/creates. Add one consistent error response shape.

When a suggestion is accepted, update the order to `REASSIGNED` and its assigned agent. Do not perform this assignment when a suggestion is merely created.

## 7. Routing design

```java
interface RoutingStrategy {
    RoutingDecision recommend(Order order, List<Agent> availableAgents, TriggerContext context);
}
```

The returned decision should contain the recommended agent, confidence, reasoning, and whether a fallback was used.

### Rule-based strategy

- Exclude `OFFLINE` agents.
- Select the agent with the fewest active orders.
- Return deterministic reasoning, for example: “Selected AGT-004 because it has the lowest active load.”
- Return a clear no-candidate result when no agent is available.

### AI strategy

Send the order, complete available-agent snapshot, and trigger context to the LLM. Request only JSON:

```json
{"agentId":"AGT-004","confidence":0.85,"reasoning":"..."}
```

Validate the response in this order:

1. Parse JSON, handling markdown fences if necessary.
2. Require `agentId`, `confidence`, and `reasoning`.
3. Clamp or reject confidence outside `0.0..1.0`.
4. Confirm `agentId` exists in the supplied available list.
5. Fall back to the rule-based strategy for timeout, quota error, malformed JSON, invalid ID, or no usable AI result.

Use separate prompt builders for initial assignment and offline recovery. The recovery prompt must explicitly include the failed agent, affected order count, and recovery context.

## 8. Agentic loop requirements

The loop should be observable in both code and the UI:

```text
Observe: AgentOfflineEvent
Reason: find orders assigned to the offline agent
Act: queue one suggestion per affected order
Checkpoint: ops accepts or rejects
```

Required safeguards:

- The status PATCH returns immediately.
- Re-planning runs asynchronously.
- Only orders assigned to the offline agent are processed.
- Existing pending `AGENT_OFFLINE` suggestions prevent duplicates.
- One failed AI call falls back to rule-based routing.
- The order remains pending until ops accepts a suggestion.
- The listener logs failures with agent/order IDs.

## 9. React UI floor

Replace the Vite starter with one operations view:

- Header showing service health and a refresh control.
- Pending reassignment list.
- Order description and current agent.
- Recommended agent and confidence.
- AI reasoning displayed verbatim.
- Distinct `AGENT_OFFLINE` re-plan badge.
- Accept and reject actions.
- Agent roster with `AVAILABLE`, `BUSY`, and `OFFLINE` states.
- Loading, empty, success, and error states.
- Poll pending suggestions every few seconds or provide a clearly usable manual refresh.

Use the existing React project rather than introducing routing or a large component library. Keep API calls in a small client module and keep the components focused on presentation and actions.

## 10. Time-boxed execution plan for 3–4 hours

### 0:00–0:10: Verify and decide

- Run backend tests/build and frontend lint/build.
- Confirm Java, Maven, Node, and npm versions.
- Decide whether to keep the existing Boot 4/React 19 versions.
- Create `ADR.md` and write the initial architecture decisions.

### 0:10–0:55: Domain and persistence

- Add enums, entities, repositories, DTOs, validation, and seed data.
- Configure H2, JPA, Actuator, and CORS.
- Add basic service/controller endpoints.
- Smoke-test order creation, order listing, agent listing, and status updates.

### 0:55–1:35: Routing engine

- Define `RoutingStrategy` and context/decision types.
- Implement rule-based routing.
- Add strategy registry/selector and configuration.
- Implement `POST /orders/{id}/suggest`.
- Test lowest-load selection and no-available-agent behavior.

### 1:35–2:20: AI and resilience

- Add the LLM gateway/provider configuration.
- Implement distinct initial and recovery prompt builders.
- Parse and validate structured JSON.
- Add fallback for every expected failure mode.
- Test with a fake/stub gateway before relying on a live key.

### 2:20–2:55: Agentic re-planning

- Publish an offline event.
- Add async listener and affected-order query.
- Add idempotency check.
- Verify repeated offline updates do not duplicate pending suggestions.
- Verify the endpoint responds without waiting for re-planning.

### 2:55–3:30: React floor

- Build the pending suggestions view and agent roster.
- Wire polling/refresh and accept/reject actions.
- Add offline re-plan badge and error/loading states.
- Exercise the full browser flow.

### 3:30–4:00: Submission hardening

- Complete ADR entries and deliberate exclusions.
- Add focused tests for state transitions, fallback, and idempotency.
- Write README setup and demo commands.
- Record the five-minute demo.
- Run final backend/frontend builds and check that no API key is committed.

If time drops below three hours, remove the live LLM dependency from the critical path by making rule-based routing the reliable default and use a configurable fake/stub for the demo. Do not drop the agentic loop, idempotency, or ADR.

## 11. Testing checklist

### Backend

- Application starts and Actuator health is `UP`.
- Invalid order and status requests return structured `400` responses.
- Missing IDs return `404`.
- Rule strategy selects the lowest-load available agent.
- Offline agents are never recommended.
- AI response with a valid ID is persisted.
- Malformed AI response falls back.
- Hallucinated agent ID falls back.
- LLM timeout/quota error falls back.
- Offline transition creates suggestions asynchronously.
- Repeated offline transition creates no duplicate pending suggestion.
- Accepting a suggestion reassigns the order.
- Rejecting a suggestion leaves the order awaiting reassignment.

### Demo path

1. Start the backend and frontend.
2. Show seeded agents and orders.
3. Set an assigned agent to `OFFLINE`.
4. Refresh or wait for polling.
5. Show the `AGENT_OFFLINE` badge, recommendation, confidence, and reasoning.
6. Accept one suggestion and reject another.
7. Show that the order and agent state reflect the decision.
8. Briefly explain the routing interface, fallback, async event, and idempotency guard.

## 12. ADR entries to maintain during implementation

Write each entry immediately after the decision:

1. Where routing logic lives and why.
2. How strategy selection works and how a third strategy will plug in.
3. How the system behaves when the LLM is unavailable.
4. How the offline event is decoupled from the request path.
5. What is extensible for sprint 2/3 and what was deliberately deferred.

Every entry should use:

`Context -> Options considered -> Decision -> Tradeoffs accepted`

## 13. Submission checklist

- `README.md` starts the backend and frontend in under five minutes.
- `ADR.md` reflects the actual implementation.
- No API keys or local secrets are committed.
- Seed data makes the re-plan demo repeatable.
- Public repository contains backend and frontend.
- Five-minute video shows the offline re-plan path, not just static screens.
- Final build/test commands pass.

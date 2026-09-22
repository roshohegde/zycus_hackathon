# Architecture Decision Record

## ADR-1: Keep routing behind a strategy contract

### Context
Routing is called by both the on-demand HTTP endpoint and the asynchronous agent-offline recovery loop. Sprint 2 is expected to add zone-aware routing, so routing logic must not be embedded in controllers or event handling code.

### Options considered
- Put selection logic in `SuggestionService`, which is simple but couples persistence and routing.
- Use a `RoutingStrategy` contract with independent implementations and a selector.
- Use a factory switch statement, which is explicit but requires editing the factory for every new strategy.

### Decision
Use `RoutingStrategy`, with `RuleBasedRoutingStrategy` and `AiRoutingStrategy` implementations. `SuggestionService` owns the application workflow, while each strategy owns recommendation logic.

### Tradeoffs accepted
There are more small classes and the strategy contract must be understood by new contributors. In return, both call paths share the same behavior and a future `ZoneAffinityStrategy` can be added without modifying the application workflow.

## ADR-2: Switch routing strategy through a registry and runtime selector

### Context
The brief requires rule-based and AI strategies and scores changing the active strategy without a code change or restart.

### Options considered
- A qualifier or fixed injected strategy, which is easy to read but static.
- A manual factory with a switch, which is explicit but violates the open-for-extension goal.
- A Spring bean map keyed by strategy name plus a runtime selector.

### Decision
Spring injects the strategy map using bean names `rule-based` and `ai`. `RoutingStrategySelector` validates and stores the active name in an `AtomicReference`; `PATCH /routing/strategy` changes it without restarting the application.

### Tradeoffs accepted
The strategy name is validated at runtime rather than compile time. The selector exposes a small operational control endpoint, so access control would be required before production use.

## ADR-3: Treat AI output as untrusted input

### Context
The model can time out, return malformed JSON, produce an invalid confidence, or hallucinate an agent ID. The reassignment workflow must continue even when the provider is unavailable.

### Options considered
- Trust the model response and persist it, which is fast but unsafe.
- Fail the whole request or async job, which protects data but drops recovery suggestions.
- Parse and validate the response, then fall back to deterministic routing.

### Decision
`AiRoutingStrategy` requests a strict JSON shape, removes optional markdown fences, parses the response, validates the agent against the available roster, validates confidence in `0.0..1.0`, and requires reasoning. Any failure logs the cause and delegates to `RuleBasedRoutingStrategy`.

### Tradeoffs accepted
Fallback recommendations may be less context-aware than AI recommendations. The system favors a visible, explainable suggestion over silently losing a stranded order.

## ADR-4: Publish an offline event and re-plan asynchronously

### Context
Changing an agent to `OFFLINE` must return quickly, while affected orders need independent recommendations. A repeated offline event must not duplicate pending recovery suggestions.

### Options considered
- Re-plan inside the PATCH request, which is straightforward but blocks on every order and LLM call.
- Poll periodically, which can miss the immediacy of the state change and adds unnecessary work.
- Publish `AgentOfflineEvent` and handle it with `@Async` on a bounded executor.

### Decision
`AgentService` publishes an event when the status becomes `OFFLINE`. `ReplanningService` asynchronously loads only affected assigned or pending orders, checks for an existing pending `AGENT_OFFLINE` suggestion, and queues one suggestion per order. The human checkpoint remains suggestion approval; the system never auto-assigns.

### Tradeoffs accepted
Async work can finish after the HTTP response and failures need logs and monitoring. The current session uses H2 and an in-process executor; a durable queue and retry policy are future production work.

## ADR-5: Deliberate scope and extensibility

### Context
The session is time-boxed and the rubric prioritizes the reassignment loop, fallback behavior, and walkthrough over optional UI and deployment features.

### Decision
The model includes nullable extension points for zones, capacity, and SLA-related fields, and the routing interface is the extension seam for `ZoneAffinityStrategy`. SSE streaming, authentication, a full dispatch map, PostgreSQL deployment, and proactive SLA monitoring are deliberately deferred.

### Tradeoffs accepted
The UI is an operations floor rather than a complete dispatch product. This preserves time for correctness of the event loop, idempotency, AI resilience, tests, and the demo path.

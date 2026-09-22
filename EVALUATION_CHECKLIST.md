# Evaluation Evidence Checklist

This file maps the hackathon scoring rubric to implementation evidence and repeatable checks.

| Rubric area | Evidence | Verification |
|---|---|---|
| Entity design, 8 pts | `domain/Agent`, `domain/Order`, `domain/ReassignmentSuggestion`; enum state machines; nullable zone/capacity fields | `mvn test`; inspect entity mappings |
| API correctness, 7 pts | Controllers, request DTO validation, `ApiExceptionHandler` | `mvn test`; `README.md` API commands |
| Persistence, 5 pts | Spring Data repositories, H2 schema, seeded data | Start backend; `GET /agents`, `GET /orders`; `GET /actuator/health` |
| Routing contract, 10 pts | `RoutingStrategy`, rule-based and AI implementations | `RoutingStrategyTests` |
| Runtime switching, 8 pts | Strategy bean map, `RoutingStrategySelector`, `PATCH /routing/strategy` | `RoutingStrategyTests.selectorCanSwitchWithoutRestart` |
| Pattern justification, 7 pts | ADR-1 and ADR-2 in `ADR.md` | Read ADR entries against implementation |
| Initial AI prompt, 8 pts | `AiRoutingStrategy.buildPrompt` includes order and roster | `POST /orders/{id}/suggest` with `ROUTING_STRATEGY=ai` |
| Recovery AI prompt, 8 pts | Recovery branch includes failed agent and stranded-order context | Trigger agent `OFFLINE`; inspect re-plan suggestion and code path |
| AI resilience, 9 pts | JSON parsing, fence removal, ID/confidence/reasoning validation, fallback | `RoutingStrategyTests` valid, malformed, hallucinated, invalid-confidence cases |
| Agentic trigger, 7 pts | `AgentOfflineEvent`, `@EventListener`, `@Async`, bounded executor | `AgentServiceTests`; offline browser/API flow |
| Agentic checkpoint, 8 pts | Suggestions remain pending until explicit accept/reject | `SuggestionServiceTests`; UI accept/reject flow |
| Ops interface floor, 12 pts | React queue, reasoning, confidence, badge, roster, polling, errors | `npm run build`, `npm run lint`, browser demo |
| ADR quality, 10 pts | ADR-1 through ADR-5 with context/options/decision/tradeoffs | Review `ADR.md` |
| Live walkthrough, 10 pts | Repeatable seeded order -> offline -> re-plan -> approval path | Follow `README.md` demo path |
| UI ceiling, +8 pts | Deliberately deferred; scope rationale in ADR-5 | Confirm no backend correctness was traded for optional UI |
| SSE bonus, +5 pts | Deliberately deferred | Confirm documented scope decision |

## Required verification commands

```bash
mvn test
cd hackathon-ui
npm run build
npm run lint
```

## Core demo assertions

- Agent status PATCH returns an agent response immediately.
- Offline orders receive one pending suggestion each.
- Repeating the same offline event does not duplicate pending suggestions.
- AI suggestions contain a valid available agent, confidence, and reasoning.
- AI failures still create rule-based suggestions.
- Accepting a suggestion changes the order to `REASSIGNED`.
- Rejecting a suggestion leaves the order assigned and resolves only the suggestion.
- A resolved suggestion cannot be changed again.
- The UI displays the offline trigger badge and AI reasoning verbatim.

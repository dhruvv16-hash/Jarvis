# JARVIS DATA FLOW

1. **USER INPUT**: A raw text/voice string is received (e.g. "Prepare my trip to Delhi").
2. **REQUEST NORMALIZATION**: Parsed into a structured `AgentRequest`.
3. **GOAL RESOLUTION**: `AgentRuntime` generates a top-level `Objective`.
4. **PERSONAL CONTEXT RETRIEVAL**: `MemoryRetrievalEngine` matches alias "Delhi" or user preference "morning flight" into a strict bounded context without polluting the LLM window.
5. **STRATEGIC PLANNING**: The LLM parses the bounds and outputs a structural `TaskGraph` featuring `PlanTask` nodes.
6. **TACTICAL DECOMPOSITION**: Node 1 generates a specific `ToolCall`.
7. **POLICY VALIDATION**: `PolicyEngine` intercepts to confirm operations. High-risk operations stall execution (`WAITING_FOR_USER_CONFIRMATION`).
8. **CAPABILITY ROUTING**: Validated calls route through `CapabilityRouter` matching internal models (e.g., `amazon.search`).
9. **DRIVER DISPATCH**: Intent or Accessibility hooks are selected based on reliability weights.
10. **ANDROID EXECUTION**: Driver performs click/scroll/intent broadcasts.
11. **OBSERVATION**: The screen updates; new semantic hierarchy is parsed.
12. **VERIFICATION**: `ObjectiveVerifier` analyzes result against node criteria.
13. **MEMORY UPDATE**: Meaningful deviations/successes update persistent history (`MemoryManager`).

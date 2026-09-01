# Phase 3 Dependency Graph

```text
AgentRuntime
   ?
AgentLoop
   ?
AgentContextProvider  (Interface)
   ? (implemented by ContextBuilder)
   ? MemoryManager
   ? AppRegistry
   ? ExecutionHistoryRepository
   ? SessionRepository
   ? TaskRepository

AgentPlanner          (Interface)
   ? (implemented by Planner)
   ? LanguageModel    (Interface)
     ? LocalLlmEngine (Implementation)
   ? ToolRegistry

AgentToolExecutor     (Interface)
   ? (implemented by CapabilityRouterAdapter)
   ? CapabilityRouter
     ? PolicyEngine
     ? DriverRegistry
       ? AccessibilityDriver
       ? IntentDriver
         ? ActionExecutor
```

### Rationale
- **Interfaces**: Enable robust testing without Android Contexts, Room databases, or complex mocking libraries. 
- **AgentLoop**: Operates solely on pure JVM memory logic, decoupled from Android constraints.
- **Model Isolation**: LanguageModel abstracts away specific underlying GenAI models (Gemma vs Gemini), enabling cloud/local swaps dynamically.

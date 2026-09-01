# Phase 2 Execution Architecture

## Goal
Transform the JARVIS execution pipeline from deterministic hard-coded paths (e.g., AmazonOrderFlow) into an abstract, model-agnostic capability execution framework.

## The Separation of Concerns
- **WHAT**: The future Planner decides the **Tool** to call (e.g., TapTool).
- **WHERE**: The **App Registry** tells the system what software exists and which capabilities they provide.
- **HOW**: The **Execution Driver** defines the means of manipulation (Accessibility, Intents, DeepLinks, Native API).
- **LOW-LEVEL ACTION**: The **ActionExecutor** remains the primitive automation wrapper around JarvisAccessibilityService.
- **RESULT**: The system returns a structured **ToolResult** / **Observation** to the agent instead of a raw Android object.

## Flow Diagram
`	ext
                    +------------------+
                    ¦   FUTURE AGENT   ¦
                    +------------------+
                             ¦
                             ?
                       ToolRegistry
                             ¦
                             ?
                    CapabilityRouter
                             ¦
                +------------+-------------+
                ?            ?             ?
             Driver A      Driver B      Driver C
                ¦            ¦             ¦
          Android API      Intent      Accessibility
                                             ¦
                                             ?
                                       ActionExecutor
                                             ¦
                                             ?
                                          Android
`

## Security & Policy Integration
The CapabilityRouter integrates tightly with the PolicyEngine. Before any driver attempts an action, the policy classifies the request (LOW, MEDIUM, HIGH, CRITICAL). If a request requires user confirmation, the system returns a WAITING_FOR_USER signal, enabling the future agent loop to gracefully halt execution until the UI resolves the user decision.

## Persistence
All ToolCall executions are recorded through the ExecutionHistoryRepository ensuring persistent context.

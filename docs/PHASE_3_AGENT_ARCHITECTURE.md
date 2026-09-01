# Phase 3 Agent Architecture

## Overview
Phase 3 transitions JARVIS from hard-coded deterministic flows into an autonomous **Observe-Reason-Act** Agent loop.
The LLM (Planner) takes the user's high-level goal, ingests context (from Phase 1 memory, current observations, and history), and emits structured ToolCalls. These calls are validated, routed (Phase 2), and executed via the ActionExecutor.

## Components
- **Goal**: Structured representation of the user's intent.
- **AgentRequest / AgentResponse**: Boundary models entering and exiting the AgentRuntime.
- **ContextBuilder**: Selectively retrieves memories, apps, capabilities, and observations to bound the LLM's context window.
- **Planner**: The core reasoning wrapper around the LanguageModel. Outputs PlannerDecision (ToolCall, Complete, AskUser, Failure).
- **AgentLoop**: A bounded state machine (while loop) coordinating ContextBuilder -> Planner -> Router -> Verifier up to maxTurns.
- **ModelRouter & LanguageModel**: Interface separating Local Gemma from Cloud implementations, enforcing structured outputs.

## Architecture Diagram
`	ext
                          USER
                           ¦
                    Voice / Text / Event
                           ¦
                           ?
                     AgentRequest
                           ¦
                           ?
                    +-------------+
                    ¦ AgentRuntime¦
                    +-------------+
                           ¦
                     ContextBuilder
                           ¦
        +------------------+------------------+
        ?                  ?                  ?
      Memory           AppRegistry          Session
        ¦                  ¦                  ¦
        +------------------+------------------+
                           ?
                        Planner
                           ¦
                           ?
                       ToolCall
                           ¦
                           ?
                    Policy / Approval
                           ¦
                           ?
                  CapabilityRouter
                           ¦
                     DriverRegistry
                           ¦
            +--------------+---------------+
            ?              ?               ?
        Android API      Intent      Accessibility
                                        ¦
                                        ?
                                  ActionExecutor
                                        ¦
                                        ?
                                      Android
                                        ¦
                                        ?
                                    Observation
                                        ¦
                                        ?
                                    Verification
                                        ¦
                           +-------------------------+
                           ?                         ?
                         Done                     Replan
                                                     ¦
                                                     +---? Planner
`

## Security & Halucination Protection
- **No Raw Coordinates**: The LLM operates on semantic observations.
- **Policy Enforcement**: CapabilityRouter runs PolicyEngine.evaluate(toolCall) blindly before anything hits the UI.
- **Prompt Injection Defense**: App content is explicitly isolated as untrusted blocks in the ContextBuilder.
- **Context Budgeting**: Memories and histories are clipped to avoid context exhaustion and memory bloat.

## Testing
Comprehensive unit testing through mocked ContextBuilders and simulated FakeLanguageModel traces validates the deterministic stability of the agent loops (verifying confirmations, success, and bounded failure retries).

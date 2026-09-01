# Matrix OS Attribution and Reuse

## Purpose
This document tracks the integration of architectural concepts from the Matrix OS reference repository into the JARVIS Android application. As JARVIS evolves into a persistent, adaptive, phone-native AI agent, it draws inspiration from Matrix OS's server-based AI architecture.

## Matrix OS Components Studied
- **AI Kernel**: Claude Agent SDK-based reasoning loop and multi-turn coordination.
- **System Prompt Assembly**: Dynamic generation of context involving state, skills, memory, and filesystem.
- **Memory Subsystem**: SQLite FTS5-backed persistence for facts, preferences, and instructions.
- **Hooks & Approval Policy**: Intercepting agent actions to enforce safety and prompt user verification.
- **Task Queue & IPC**: SQLite-backed agent-to-agent coordination.
- **App Model & State**: Manifest-driven applications interacting with structured storage.
- **Conversation History**: Summarization and session continuity.

## Licensing & Code Reuse
- **Matrix OS License Status**: (Reference Only) Matrix OS is studied for its architectural insights.
- **Direct Source Code Copying**: **None.** No TypeScript, Node.js, or server-side implementation code from Matrix OS has been directly copied into the JARVIS codebase.
- **Phase 1 Update**: All persistent concepts (Memory, Apps, History, Sessions) have been implemented from scratch natively using Android Room (SQLite). Matrix OS uses TypeScript and Node SQL drivers; none of this was copied.
- **Independent Reimplementation**: All inspired concepts are implemented natively in Kotlin specifically for the Android environment (e.g., using Room/SQLite instead of Node SQL drivers, Coroutines instead of Promise chains, and Android services). 
- **Legal/IP Consideration**: The relationship is entirely "inspired by," focusing on patterns (like separating execution drivers from capability intent) rather than adapting identical syntax. Legal review should confirm that reimplementing the conceptual design (adaptive agent loop, memory store interfaces) in a different programming ecosystem independently satisfies IP requirements.

## Reimplemented Concepts
1. **Agent Runtime**: Replaces hard-coded flows. Inspired by Matrix OS's `spawnKernel()`.
2. **Context Builder**: Inspired by `buildSystemPrompt()`, pulling from memory, state, and registered apps.
3. **Memory Store**: Inspired by Matrix OS's FTS memory, adapted for Android SQLite/Room.
4. **Security/Policy Engine**: Inspired by Matrix OS hooks (`safetyGuardHook`, `createApprovalHook`).
5. **App Model**: Evolved from Matrix OS's `~/apps/` directories into an Android-aware `AppRegistry` maintaining descriptors and learned capabilities.

## What Remains External / Reference-Only
The following Matrix OS elements are explicitly NOT integrated into JARVIS:
- Web-based Canvas shell / Next.js renderer.
- Electron desktop shell.
- Server-side VPS provisioning, user auth, and multi-tenant billing models.
- Dependency on the Anthropic Claude Agent SDK as the only internal orchestrator.
- Gateway HTTP/WebSocket adapters (until a multi-channel UI dictates otherwise).

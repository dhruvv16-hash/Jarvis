# Memory Write Integration Report

## 1. Problem
The previous implementations (Phases 1-11) provided the conceptual structure for memory management and Agent execution. However, they lacked a direct wiring path to persist structured user preferences explicitly triggered from natural language into the persistent Room database.

## 2. Existing Architecture
Prior to this integration, the LLM integration layer (LanguageModel) emitted a simple String 	ype ("tool_call", "ask_user", etc.). MemoryManager had a duplicate namespace collision between an interface abstraction and a repository implementation.

## 3. New Memory Decision
The model output protocol was refactored from a weakly typed ModelOutput class to a strict ModelDecision sealed class. A native ModelDecision.MemoryWrite type was added to support structured preference extractions directly from the LLM, containing category, scope, confidence, and source.

## 4. Validation & Tooling
A new domain tool, SaveMemoryTool, was created and registered with ToolRegistry. This tool intercepts the raw LLM structured data, strictly parses MemoryCategory and MemoryScope enums, and surfaces descriptive errors for malformed requests.

## 5. Provenance
A RequestSource enum was added to AgentRequest. When SaveMemoryTool processes a memory write indicating MemorySource.USER_EXPLICIT, it explicitly verifies that the RequestSource of the triggering loop was either USER_DIRECT or USER_VOICE. Content derived from app notifications or system events is strictly rejected if it attempts to forge explicit user consent.

## 6. Persistence
The duplicate MemoryManager conflict was resolved by merging com.jarvispoc.data.manager.MemoryManager to implement com.jarvispoc.memory.MemoryManager. The underlying schema of MemoryEntity was safely extended to persist scope, capabilityId, and ctive status natively into the SQLite (Room) repository.

## 7. Deduplication
Deduplication logic in MemoryManager ensures that repeatedly storing the same value for the same capability/scope merely performs an updatedAt touch instead of inflating database size.

## 8. Scope & Context Retrieval
ContextBuilder.kt was augmented to retrieve relevant capability preferences. When evaluating an objective, MemoryManager.getRelevantPreferences isolates the search using MemoryScope.CAPABILITY. This ensures "Cash on Delivery" for "shopping.order" capabilities is retrieved when building the prompt context for shopping, but excluded for music playing.

## 9. Planner Integration
The planner intercepts ModelDecision.MemoryWrite and translates it into an actionable internal ToolCall pointing to save_memory, allowing the existing AgentLoop tool executor to run it gracefully.

## 10. Real Device & Database Evidence
The implementation is validated by MemoryWriteIntegrationTest, proving end-to-end integration from unstructured AgentRequest through Planner decision, into SaveMemoryTool validation, and finally writing to the physical MemoryRepository SQLite mock instance.

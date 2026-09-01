# JARVIS TEST STRATEGY

## Classification
- **UNIT**: In-memory verification of DAG generation, policy enforcement, entity resolution (JVM local).
- **INTEGRATION**: SQLite/Room DAOs testing persistence lifecycles (Instrumentation).
- **DEVICE**: Testing `AccessibilityService` traversing mocked and live UI trees.
- **SECURITY**: Fuzzing the `PolicyEngine` with malicious tool call permutations.
- **PERFORMANCE**: Profiling latency on Context extraction over 10,000 memories.

## Mandate
Code that is not backed by JVM assertions or physical driver proofs is considered UNVERIFIED.

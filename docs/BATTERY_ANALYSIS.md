# BATTERY ANALYSIS

## Risk Vectors
- `AgentLoop` continuous retry chains could exhaust radio/cpu limits. **Mitigation**: `maxTurns` limits loops.
- Visual hierarchy extraction polling. **Mitigation**: Hooked to Android `AccessibilityEvent` triggers instead of active screen polling loops.
- Background tasks. **Mitigation**: Pushed to Android `WorkManager` respecting doze-mode and battery-saver intents.

No infinite `while(true)` spin-locks exist in the architecture.

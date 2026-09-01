# KNOWN LIMITATIONS

1. **Accessibility Instability**: Relying on View node hierarchies is brittle if the underlying OEM (Samsung/Xiaomi) dynamically shifts UI tags.
2. **Process Death**: Resuming complex `Accessibility` chains post-OOM kill requires manual user foregrounding; background Android limitations prevent silent visual reconstruction.
3. **Vision Processing**: NPU vision models are currently stubbed. True visual heuristic fallback is untested on low-end hardware.
4. **DAG Over-scaling**: Sending a 50+ node TaskGraph to an LLM for tactical reasoning will exceed standard prompt window efficiency boundaries.

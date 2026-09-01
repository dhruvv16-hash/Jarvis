# JARVIS NEXT-GEN

JARVIS is a phone-native AI agent that understands goals, uses Android capabilities and applications, remembers useful context, learns verified procedures, adapts to changing interfaces, and can continue work across sessions and background events while remaining constrained by Android permissions and explicit safety policies.

## Features
- **Hierarchical Planning**: Breaks complex objectives into non-blocking TaskGraphs.
- **Personal Memory**: Privately stores preferences and relationships strictly on-device using SQLite.
- **Policy Constrained**: Enforces human confirmation for high-risk device operations.
- **Background Capable**: Resumes and triggers automations gracefully matching OS limits.

## Architecture
Built natively for Android utilizing Room, Kotlin Coroutines, and strict architectural bounds separating LLM inference from privileged Accessibility execution.

## Testing
Run the local JVM suite:
`./gradlew testDebugUnitTest`

## Known Limitations
True UI adaptability requires physical device execution to map dynamic OEM view hierarchies perfectly.

*Note: This is a Release Candidate.*

# DATA PRIVACY MODEL

## 1. Storage
- **Local First**: All Room databases (`Memory`, `Session`, `History`) live inside app sandboxing.
- **Network Boundaries**: Outbound network requests occur exclusively within `LanguageModel` abstractions and explicit App API integrations.

## 2. Retention & Deletion
- `Temporary` scoped memories self-invalidate.
- Users maintain direct capability to `forgetPreference(id)` removing facts entirely from the context generator.

## 3. Exclusion
- Passwords, Banking metrics, and highly secure credentials are structurally blacklisted from Memory insertion routines via `PolicyEngine` classifiers.

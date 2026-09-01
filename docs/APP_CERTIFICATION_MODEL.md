# App Certification Model

Certifications are non-binary boundaries mapped against specific environmental snapshots:
- App Version
- Android SDK Version
- Driver Profile (Accessibility vs DeepLink)

## Regression States
An app update intrinsically flags prior certifications as `STALE`. `RegressionDetector` loops continuously monitor failure taxonomies (`UI_CHANGED`, `TARGET_NOT_FOUND`) rolling back certification layers when confidence decays beneath accepted bounds.

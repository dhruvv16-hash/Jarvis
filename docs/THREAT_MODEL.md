# THREAT MODEL

| Threat Vector | Mitigation Strategy | Status |
|---------------|---------------------|--------|
| App Prompt Injection | `PolicyEngine` intercepts resulting tool calls, forcing User Confirmation for sensitive operations. | IMPLEMENTED |
| Malicious APK Download | Model cannot circumvent Android PackageManager nor provide silent install URIs. | IMPLEMENTED |
| Unbounded Background | `DeviceInteractionLockOwner` prevents background jobs from stealing active UI focus. | IMPLEMENTED |
| Infinite Loop (Denial of Service) | DFS validation rejects cyclical DAGs pre-execution. | IMPLEMENTED |
| Stolen Device | Agent relies on underlying Android biometric/keystore locks. | PLATFORM |

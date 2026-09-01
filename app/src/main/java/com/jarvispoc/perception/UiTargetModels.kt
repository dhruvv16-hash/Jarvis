package com.jarvispoc.perception

import com.jarvispoc.apps.onboarding.UiElement

data class UiTarget(
    val semanticRole: String?,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val className: String?,
    val screenRole: String?,
    val parentRole: String?,
    val indexHint: Int?,
    val boundsHint: Map<String, Int>?,
    val state: Map<String, Boolean>?,
    val appId: String?,
    val confidence: Float
)

data class TargetCandidate(
    val uiTarget: UiTarget,
    val resolvedNode: UiElement?,
    val matchReasons: List<String>,
    val confidence: Float,
    val driver: String
)

enum class ActionOutcome {
    INTENT, DISPATCHED, OBSERVED, VERIFIED, UNKNOWN_OUTCOME, VERIFICATION_FAILED
}

data class ActionReceipt(
    val executionId: String,
    val requestedAction: String,
    val driver: String,
    val timestamp: Long,
    val observation: String?,
    val outcome: ActionOutcome
)

data class ScreenSignature(
    val packageId: String,
    val activity: String?,
    val screenRole: String,
    val majorLabels: List<String>,
    val interactiveRoles: List<String>,
    val navigationState: String?
)

data class UiStateTransition(
    val sourceSignature: ScreenSignature,
    val action: String,
    val targetSignature: ScreenSignature,
    val verification: String,
    val confidence: Float,
    val appVersion: String?
)

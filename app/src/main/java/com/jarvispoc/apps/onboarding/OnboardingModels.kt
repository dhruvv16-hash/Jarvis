package com.jarvispoc.apps.onboarding

enum class ScreenRole {
    HOME, SEARCH, SEARCH_RESULTS, PRODUCT, DETAIL, CART, CHECKOUT, 
    COMPOSER, MESSAGE, PROFILE, LOGIN, SETTINGS, PERMISSION, ERROR, UNKNOWN
}

data class UiElement(
    val elementId: String,
    val role: String,
    val text: String?,
    val contentDescription: String?,
    val semanticDescription: String?,
    val bounds: Map<String, Int>?,
    val enabled: Boolean,
    val clickable: Boolean,
    val editable: Boolean,
    val selected: Boolean,
    val confidence: Float
)

enum class HypothesisStatus {
    HYPOTHESIZED, TESTING, VERIFIED, REJECTED, STALE
}

data class CapabilityHypothesis(
    val id: String,
    val capabilityId: String,
    val appId: String,
    val evidence: List<String>,
    val confidence: Float,
    val status: HypothesisStatus,
    val createdAt: Long,
    val updatedAt: Long
)

enum class OnboardingSessionStatus {
    STARTED, INSPECTING, DISCOVERING, TESTING, COMPLETED, PARTIAL, FAILED, CANCELLED
}

data class AppOnboardingSession(
    val id: String,
    val appId: String,
    val taskId: String,
    val sessionId: String,
    val goal: String,
    var status: OnboardingSessionStatus,
    val startedAt: Long,
    var completedAt: Long?,
    var actionsUsed: Int,
    var screensVisited: Int,
    val capabilitiesDiscovered: MutableList<String>
)

enum class AppOnboardingResultStatus {
    SUCCESS, PARTIAL, REQUIRES_USER, UNSUPPORTED, FAILED
}

data class AppOnboardingResult(
    val status: AppOnboardingResultStatus,
    val capabilitiesDiscovered: List<String>,
    val proceduresLearned: List<String>,
    val limitations: List<String>,
    val nextAction: String?
)

data class ExplorationBudget(
    val maxActions: Int = 10,
    val maxDurationMs: Long = 60000,
    val maxScreenTransitions: Int = 15,
    val maxModelCalls: Int = 5
)

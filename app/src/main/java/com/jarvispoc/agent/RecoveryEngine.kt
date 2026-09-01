package com.jarvispoc.agent

interface RecoveryEngine {
    suspend fun attemptRecovery(failure: Any): Any
}

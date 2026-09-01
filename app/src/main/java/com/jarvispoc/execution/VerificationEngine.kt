package com.jarvispoc.execution

interface VerificationEngine {
    suspend fun verify(actionResult: Any, criteria: Any): Boolean
}

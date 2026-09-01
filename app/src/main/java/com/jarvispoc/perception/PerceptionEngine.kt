package com.jarvispoc.perception

interface PerceptionEngine {
    suspend fun observeCurrentState(): Observation
}

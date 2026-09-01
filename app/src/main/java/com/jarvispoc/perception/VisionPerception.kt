package com.jarvispoc.perception

interface VisionPerception {
    suspend fun analyzeScreen(): Observation
}

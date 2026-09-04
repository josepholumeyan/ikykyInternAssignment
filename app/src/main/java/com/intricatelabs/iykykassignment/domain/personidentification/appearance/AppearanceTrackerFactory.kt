package com.intricatelabs.iykykassignment.domain.personidentification.appearance

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceTrackerFactory @Inject constructor() {
    fun create(
        continuityThreshold: Float = 0.5f,
        maxGapMs: Long = 500L
    ): AppearanceTracker {
        return AppearanceTracker(continuityThreshold, maxGapMs)
    }
}
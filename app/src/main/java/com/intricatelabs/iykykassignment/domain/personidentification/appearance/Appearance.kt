package com.intricatelabs.iykykassignment.domain.personidentification.appearance

data class Appearance(
    val startMs: Long,
    var lastSeenMs: Long,
    val faceIds: MutableList<Long> = mutableListOf(),
    val embeddings: MutableList<FloatArray> = mutableListOf()
)
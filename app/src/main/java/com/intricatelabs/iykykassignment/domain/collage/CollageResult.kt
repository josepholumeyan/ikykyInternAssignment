package com.intricatelabs.iykykassignment.domain.collage

data class CollageResult(
    val collageImagePath: String,
    val people: List<PersonSummary>
)
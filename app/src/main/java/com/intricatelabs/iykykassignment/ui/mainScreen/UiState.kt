package com.intricatelabs.iykykassignment.ui.mainScreen

data class PersonResult(
    val personId: Long,
    val representativeImagePath: String, // local path/URI to the cropped representative shot
    val appearanceCount: Int
)

enum class ProcessingStep(val title: String, val activeSubtitle: String, val idleSubtitle: String) {
    DETECTING_FACES("Detecting faces", "Scanning frames", "Scanning frames"),
    IDENTIFYING_PEOPLE("Identifying people", "Extracting face embeddings", "Groups the same person together"),
    SELECTING_BEST_SHOTS("Selecting best shots", "Evaluating quality", "Picks the clearest, most flattering shot"),
    CREATING_COLLAGE("Creating collage", "Arranging and composing", "Builds a beautiful collage")
}

sealed interface UiState {
    data object Idle : UiState

    data class Processing(
        val overallProgress: Float, // 0f..1f
        val currentStep: ProcessingStep
    ) : UiState

    data class Results(
        val videoName: String,
        val people: List<PersonResult>,
        val collageImagePath: String? // final composed collage file, once written
    ) : UiState

    data class Error(
        val message: String = "We couldn't process your video. Please try again."
    ) : UiState
}

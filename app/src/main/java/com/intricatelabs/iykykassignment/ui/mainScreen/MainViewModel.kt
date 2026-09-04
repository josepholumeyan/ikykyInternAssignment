package com.intricatelabs.iykykassignment.ui.mainScreen

import android.content.Context
import android.util.Log
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intricatelabs.iykykassignment.data.AppDatabase
import com.intricatelabs.iykykassignment.domain.GalleryExporter
import com.intricatelabs.iykykassignment.domain.collage.CollageOrchestrator
import com.intricatelabs.iykykassignment.domain.faceDetection.FaceDetectionOrchestrator
import com.intricatelabs.iykykassignment.domain.personidentification.PersonIdentificationOrchestrator
import com.intricatelabs.iykykassignment.domain.personidentification.RepresentativeShotSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val faceDetectionOrchestrator: FaceDetectionOrchestrator,
    private val personIdentificationOrchestrator: PersonIdentificationOrchestrator,
    private val representativeShotSelector: RepresentativeShotSelector,
    private val collageOrchestrator: CollageOrchestrator,
    private val galleryExporter: GalleryExporter,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // One-shot events for actions that need an Activity context (launching
    // an intent) rather than a persistent state value — the ViewModel
    // shouldn't hold an Activity, so it just announces "here's a URI to
    // share" / "here's whether the save worked" and lets the Composable,
    // which does have context, react.
    private val _shareEvent = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val shareEvent: SharedFlow<Uri> = _shareEvent

    private val _saveResultEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val saveResultEvent: SharedFlow<Boolean> = _saveResultEvent

    private var lastCollagePath: String? = null

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            appDatabase.clearVideoData(uri.toString())
            val videoUriString = uri.toString()
            _uiState.value = UiState.Processing(0f, ProcessingStep.DETECTING_FACES)

            try {
                Log.i("MainViewModel","Calling facedetector at ${System.currentTimeMillis()}")
                faceDetectionOrchestrator.process(uri) { percent ->
                    _uiState.value = UiState.Processing(percent / 100f, ProcessingStep.DETECTING_FACES)
                }

                Log.i("MainViewModel","Calling person identifiation at ${System.currentTimeMillis()}")
                _uiState.value = UiState.Processing(0f, ProcessingStep.IDENTIFYING_PEOPLE)
                personIdentificationOrchestrator.process(videoUriString) { percent ->
                    _uiState.value = UiState.Processing(percent / 100f, ProcessingStep.IDENTIFYING_PEOPLE)
                }

                Log.i("MainViewModel","Calling rep shot select at ${System.currentTimeMillis()}")
                _uiState.value = UiState.Processing(0f, ProcessingStep.SELECTING_BEST_SHOTS)
                representativeShotSelector.selectAll(videoUriString) { percent ->
                    _uiState.value = UiState.Processing(percent / 100f, ProcessingStep.SELECTING_BEST_SHOTS)
                }

                Log.i("MainViewModel","Calling collage orc at ${System.currentTimeMillis()}")
                _uiState.value = UiState.Processing(0.2f, ProcessingStep.CREATING_COLLAGE)
                val collageResult = collageOrchestrator.createCollage(videoUriString)
                _uiState.value = UiState.Processing(1f, ProcessingStep.CREATING_COLLAGE)

                lastCollagePath = collageResult.collageImagePath

                _uiState.value = UiState.Results(
                    videoName = uri.lastPathSegment ?: "video",
                    people = collageResult.people.map {
                        PersonResult(it.personId, it.representativeImagePath, it.appearanceCount)
                    },
                    collageImagePath = collageResult.collageImagePath
                )
            } catch (e: Exception) {
                Log.e("MainViewModel","error",e)
                _uiState.value = UiState.Error(
                    e.message ?: "We couldn't process your video. Please try again."
                )
            }
        }
    }

    fun saveCollageToGallery() {
        val path = lastCollagePath ?: return
        viewModelScope.launch {
            // GalleryExporter branches internally on API level. On 26-28
            // this silently fails without WRITE_EXTERNAL_STORAGE having
            // been granted first — that runtime permission request needs
            // to happen in the Composable/Activity before this is called,
            // it isn't handled here.
            val success = galleryExporter.saveToGallery(path)
            _saveResultEvent.emit(success)
        }
    }

    fun shareCollage(context: Context) {
        val path = lastCollagePath ?: return
        val file = File(path)
        // Must match the authorities string in your manifest's <provider>
        // entry exactly — "${applicationId}.fileprovider" resolves to
        // this at build time.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        _shareEvent.tryEmit(uri)
    }

    fun reset() {
        _uiState.value = UiState.Idle
        lastCollagePath = null
    }
}
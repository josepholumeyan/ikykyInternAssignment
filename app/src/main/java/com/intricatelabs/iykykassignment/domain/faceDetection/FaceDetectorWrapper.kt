package com.intricatelabs.iykykassignment.domain.faceDetection

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * Wraps ML Kit's Face Detection API. This is the source of every
 * "representative shot" signal the assignment asks for:
 * frontality (eulerY/eulerX), eyesOpen, smiling.
 */
@Singleton
class FaceDetectorWrapper @Inject constructor() {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // eyes/smile
            .setMinFaceSize(0.1f)
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<Face> =
        suspendCancellableCoroutine { cont ->
            // FORCE hardware graphics configurations down to readable CPU pixels!
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }

            val image = InputImage.fromBitmap(safeBitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (cont.isActive) cont.resume(faces)
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetectorWrapper", "detect face failed", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }


    /** Rough 0-1 "how good is this shot" score from ML Kit signals alone. */
    fun frontalityScore(face: Face): Float {
        // Smaller head-turn angles = more frontal. Clamp + normalize.
        val yaw = abs(face.headEulerAngleY)
        val roll = abs(face.headEulerAngleZ)
        val penalty = (yaw + roll) / 90f
        return (1f - penalty).coerceIn(0f, 1f)
    }

    fun eyesOpenScore(face: Face): Float {
        val left = face.leftEyeOpenProbability ?: 0.5f
        val right = face.rightEyeOpenProbability ?: 0.5f
        return (left + right) / 2f
    }

    fun smileScore(face: Face): Float = face.smilingProbability ?: 0f
}
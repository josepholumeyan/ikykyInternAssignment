package com.intricatelabs.iykykassignment.domain.faceDetection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

object FaceAlignmentUtils {

    /**
     * Aligns a face based on eye landmarks and crops/resizes it to the target size.
     * Uses standard canonical eye positions for MobileFaceNet (112x112).
     */
    fun align(frame: Bitmap, face: Face, targetSize: Int = 112): Bitmap {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        if (leftEye == null || rightEye == null) {
            // Fallback to naive crop if landmarks are missing
            return naiveCrop(frame, face, targetSize)
        }

        val dx = rightEye.x - leftEye.x
        val dy = rightEye.y - leftEye.y
        val actualEyeDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // Canonical eye positions for a 112x112 image
        // These are standard targets for many face recognition models
        val desiredLeftEyeX = targetSize * 0.35f
        val desiredRightEyeX = targetSize * 0.65f
        val desiredEyeY = targetSize * 0.4f
        val desiredEyeDist = desiredRightEyeX - desiredLeftEyeX

        val scale = desiredEyeDist / actualEyeDist

        val matrix = Matrix()
        matrix.postTranslate(-leftEye.x, -leftEye.y)   // 1. eye to origin
        matrix.postRotate(-angle)                        // 2. rotate about that origin
        matrix.postScale(scale, scale)                   // 3. scale about that origin
        matrix.postTranslate(desiredLeftEyeX, desiredEyeY)  // 4. move to canonical position

        val alignedBitmap = createBitmap(targetSize, targetSize)
        val canvas = Canvas(alignedBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(frame, matrix, paint)

        return alignedBitmap
    }

    private fun naiveCrop(frame: Bitmap, face: Face, targetSize: Int): Bitmap {
        val box = face.boundingBox
        val left = box.left.coerceIn(0, frame.width - 1)
        val top = box.top.coerceIn(0, frame.height - 1)
        val width = box.width().coerceAtMost(frame.width - left)
        val height = box.height().coerceAtMost(frame.height - top)
        
        val crop = Bitmap.createBitmap(frame, left, top, width.coerceAtLeast(1), height.coerceAtLeast(1))
        val scaled = Bitmap.createScaledBitmap(crop, targetSize, targetSize, true)
        if (crop != scaled) crop.recycle()
        return scaled
    }
}

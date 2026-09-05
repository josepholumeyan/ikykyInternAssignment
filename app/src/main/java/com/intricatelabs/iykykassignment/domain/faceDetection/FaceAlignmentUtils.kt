package com.intricatelabs.iykykassignment.domain.faceDetection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import androidx.core.graphics.scale
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

object FaceAlignmentUtils {

    /**
     * Aligns a face based on eye landmarks and crops/resizes it to the target size.
     * Uses standard canonical eye positions for MobileFaceNet (112x112).
     */
    fun align(frame: Bitmap, face: Face, targetSize: Int = 112): Bitmap {
        val leftEyeFull = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEyeFull = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        if (leftEyeFull == null || rightEyeFull == null) {
            return naiveCrop(frame, face, targetSize)
        }

        // Pre-crop generously around the face's own bounding box FIRST.
        // Without this, when a face is large/close in the frame (common in
        // a portrait video), the scale math below shrinks the source to
        // fit the canonical eye spacing — which means the fixed 112x112
        // canvas ends up sampling a WIDER region of the original frame
        // than expected, wide enough to pull in a neighboring person
        // standing nearby. Pre-cropping bounds the maximum possible field
        // of view by construction, regardless of what the scale factor
        // turns out to be, instead of trying to tune/clamp the scale value.
        val box = face.boundingBox
        val marginFactor = 1.0f // generous room for rotation/scale to work with; tune if needed
        val marginX = (box.width() * marginFactor).toInt()
        val marginY = (box.height() * marginFactor).toInt()
        val cropLeft = (box.left - marginX).coerceIn(0, frame.width)
        val cropTop = (box.top - marginY).coerceIn(0, frame.height)
        val cropRight = (box.right + marginX).coerceIn(0, frame.width)
        val cropBottom = (box.bottom + marginY).coerceIn(0, frame.height)
        val cropWidth = (cropRight - cropLeft).coerceAtLeast(1)
        val cropHeight = (cropBottom - cropTop).coerceAtLeast(1)

        val localFrame = Bitmap.createBitmap(frame, cropLeft, cropTop, cropWidth, cropHeight)

        // Landmarks were measured in the FULL frame's coordinate space —
        // translate into the local crop's coordinate space to match.
        val leftEye = PointF(leftEyeFull.x - cropLeft, leftEyeFull.y - cropTop)
        val rightEye = PointF(rightEyeFull.x - cropLeft, rightEyeFull.y - cropTop)

        val dx = rightEye.x - leftEye.x
        val dy = rightEye.y - leftEye.y
        val actualEyeDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        val desiredLeftEyeX = targetSize * 0.35f
        val desiredRightEyeX = targetSize * 0.65f
        val desiredEyeY = targetSize * 0.4f
        val desiredEyeDist = desiredRightEyeX - desiredLeftEyeX

        val scale = desiredEyeDist / actualEyeDist

        // postXxx composes in call order (each op runs AFTER what's already
        // accumulated) — this is the corrected ordering from the earlier
        // pre/post bug: translate eye to origin, rotate about it, scale
        // about it, then translate to the canonical position.
        val matrix = Matrix()
        matrix.postTranslate(-leftEye.x, -leftEye.y)
        matrix.postRotate(-angle)
        matrix.postScale(scale, scale)
        matrix.postTranslate(desiredLeftEyeX, desiredEyeY)

        val alignedBitmap = createBitmap(targetSize, targetSize)
        val canvas = Canvas(alignedBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(localFrame, matrix, paint)

        localFrame.recycle()
        return alignedBitmap
    }

    private fun naiveCrop(frame: Bitmap, face: Face, targetSize: Int): Bitmap {
        val box = face.boundingBox
        val left = box.left.coerceIn(0, frame.width - 1)
        val top = box.top.coerceIn(0, frame.height - 1)
        val width = box.width().coerceAtMost(frame.width - left)
        val height = box.height().coerceAtMost(frame.height - top)

        val crop = Bitmap.createBitmap(frame, left, top, width.coerceAtLeast(1), height.coerceAtLeast(1))
        val scaled = crop.scale(targetSize, targetSize)
        if (crop != scaled) crop.recycle()
        return scaled
    }
}

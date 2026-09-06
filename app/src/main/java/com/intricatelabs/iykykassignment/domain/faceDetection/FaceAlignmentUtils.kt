package com.intricatelabs.iykykassignment.domain.faceDetection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.atan2
import kotlin.math.sqrt

object FaceAlignmentUtils {

    /**
     * allFacesInFrame: every OTHER detection ML Kit returned for this same
     * frame (pass rawFaces, before dedup — near-duplicate boxes of the SAME
     * face naturally overlap in x, so they never trigger the "neighbor to
     * the left/right" clamp below; only genuinely distinct, disjoint boxes
     * do).
     */
    fun align(frame: Bitmap, face: Face, allFacesInFrame: List<Face>, targetSize: Int = 112): Bitmap {
        val leftEyeFull = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEyeFull = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        if (leftEyeFull == null || rightEyeFull == null) {
            return naiveCrop(frame, face, targetSize)
        }

        val box = face.boundingBox
        val marginFactor = 1.0f
        var marginX = (box.width() * marginFactor).toInt()
        val marginY = (box.height() * marginFactor).toInt()

        // Clamp the horizontal margin so the pre-crop can never cross past
        // roughly the midpoint toward a genuinely neighboring face in the
        // SAME frame. A fixed percentage margin has no idea how close the
        // next person is standing — this is what was still letting
        // side-by-side people's aligned crops bleed into each other even
        // after bounding the crop to this face's own bbox.
        for (other in allFacesInFrame) {
            if (other === face) continue
            val otherBox = other.boundingBox

            // Only consider neighbors at roughly the same height — a face
            // far above/below isn't the side-by-side case, and clamping
            // against it would wrongly shrink margin for an unrelated
            // vertical framing coincidence.
            val verticalOverlap = box.top < otherBox.bottom && box.bottom > otherBox.top
            if (!verticalOverlap) continue

            if (otherBox.left > box.right) {
                val gap = otherBox.left - box.right
                marginX = minOf(marginX, (gap / 2).coerceAtLeast(0))
            } else if (otherBox.right < box.left) {
                val gap = box.left - otherBox.right
                marginX = minOf(marginX, (gap / 2).coerceAtLeast(0))
            }
        }

        val cropLeft = (box.left - marginX).coerceIn(0, frame.width)
        val cropTop = (box.top - marginY).coerceIn(0, frame.height)
        val cropRight = (box.right + marginX).coerceIn(0, frame.width)
        val cropBottom = (box.bottom + marginY).coerceIn(0, frame.height)
        val cropWidth = (cropRight - cropLeft).coerceAtLeast(1)
        val cropHeight = (cropBottom - cropTop).coerceAtLeast(1)

        val localFrame = Bitmap.createBitmap(frame, cropLeft, cropTop, cropWidth, cropHeight)

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
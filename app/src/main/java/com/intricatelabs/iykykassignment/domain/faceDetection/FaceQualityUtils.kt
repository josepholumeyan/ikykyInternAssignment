package com.intricatelabs.iykykassignment.domain.faceDetection

import android.graphics.Bitmap
import android.graphics.Rect

object FaceQualityUtils {

    /** Crop generously around the detected bounding box — never crop tight to it. */
    fun cropWithMargin(frame: Bitmap, box: Rect, marginFactor: Float = 0.4f): Bitmap {
        val marginX = (box.width() * marginFactor).toInt()
        val marginY = (box.height() * marginFactor).toInt()

        val left = (box.left - marginX).coerceIn(0, frame.width)
        val top = (box.top - marginY).coerceIn(0, frame.height)
        val right = (box.right + marginX).coerceIn(0, frame.width)
        val bottom = (box.bottom + marginY).coerceIn(0, frame.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(frame, left, top, width, height)
    }

    /** True if the face bbox touches (or nearly touches) the frame edge — likely a clipped face. */
    fun isClipped(box: Rect, frameWidth: Int, frameHeight: Int, edgeMarginPx: Int = 4): Boolean {
        return box.left <= edgeMarginPx ||
            box.top <= edgeMarginPx ||
            box.right >= frameWidth - edgeMarginPx ||
            box.bottom >= frameHeight - edgeMarginPx
    }

    /**
     * Laplacian-variance blur metric — higher means sharper. Plain grayscale
     * + 3x3 Laplacian convolution, no OpenCV dependency needed since this
     * only runs on small face-crop bitmaps, not full frames.
     */
    fun sharpnessScore(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 3 || h < 3) return 0f

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val gray = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val laplacian =
                    -4 * gray[idx] +
                        gray[idx - 1] + gray[idx + 1] +
                        gray[idx - w] + gray[idx + w]
                sum += laplacian
                sumSq += laplacian.toDouble() * laplacian
                count++
            }
        }

        if (count == 0) return 0f
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return variance.toFloat()
    }
}

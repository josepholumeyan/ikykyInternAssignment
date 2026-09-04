package com.intricatelabs.iykykassignment.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Persists bitmaps that need to outlive the frame/pass that created them.
 * Two use cases: individual face crops (saved during phase 1 detection, so
 * something survives past FaceDetectionOrchestrator's per-frame recycle),
 * and the final composed collage image.
 */
object ImageStorage {

    fun saveCrop(context: Context, bitmap: Bitmap, key: String): String {
        val dir = File(context.cacheDir, "face_crops").apply { mkdirs() }
        val file = File(dir, "face_$key.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Log.i("Crop path",file.absolutePath)
        return file.absolutePath
    }

    fun saveCollage(context: Context, bitmap: Bitmap, videoUri: String): String {
        val dir = File(context.cacheDir, "collages").apply { mkdirs() }
        val file = File(dir, "collage_${videoUri.hashCode()}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }
}
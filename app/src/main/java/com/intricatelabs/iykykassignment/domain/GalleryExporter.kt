package com.intricatelabs.iykykassignment.domain

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies a collage out of app-private cache storage into the device's
 * public MediaStore, so it shows up in Gallery/Photos like any other saved
 * photo. Two genuinely different code paths depending on OS version — not
 * defensive-only code, since minSdk 26 means devices on both sides of
 * Android 10's scoped-storage change are real targets.
 */
@Singleton
class GalleryExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun saveToGallery(collageFilePath: String): Boolean {
        val sourceFile = File(collageFilePath)
        if (!sourceFile.exists()) return false

        val filename = "collage_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStoreScoped(sourceFile, filename)
        } else {
            saveViaLegacyExternalStorage(sourceFile, filename)
        }
    }

    // API 29+ — no filesystem permission needed for your own app's inserts.
    private fun saveViaMediaStoreScoped(sourceFile: File, filename: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // Groups saved collages in their own subfolder instead of
            // dumping loose files into the root Pictures directory.
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iykykCollage")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false

        return try {
            val opened = resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input -> input.copyTo(out) }
            }
            if (opened == null) {
                resolver.delete(uri, null, null)
                return false
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null) // clean up the pending row on failure
            false
        }
    }

    // API 26-28 — no MediaStore scoped API yet; needs the legacy
    // WRITE_EXTERNAL_STORAGE permission (requested at runtime by the
    // caller before this is invoked — NOT handled inside this class) plus
    // the classic media-scan broadcast so Gallery notices the new file.
    @Suppress("DEPRECATION")
    private fun saveViaLegacyExternalStorage(sourceFile: File, filename: String): Boolean {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "iykykCollage").apply { mkdirs() }
        val destFile = File(appDir, filename)

        return try {
            sourceFile.copyTo(destFile, overwrite = true)
            context.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destFile))
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

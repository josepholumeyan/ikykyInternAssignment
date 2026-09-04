package com.intricatelabs.iykykassignment.domain.faceDetection

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Pulls sampled frames out of a video at a fixed interval.
 * Runs on Dispatchers.Default since decoding is CPU-bound, not I/O —
 * same reasoning as your Mowa concurrency split (IO vs Default work).
 */
class VideoFrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun extractFrames(
        videoUri: Uri,
        timeMs: Long
    ): SampledFrame? = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        var frame: SampledFrame? = null

        try {
            retriever.setDataSource(context, videoUri)

            // getScaledFrameAtTime is only available on sdk 27 and above
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val nativeWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
                val nativeHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920

                val downsampleFactor = 2
                val targetWidth = nativeWidth / downsampleFactor
                val targetHeight = nativeHeight / downsampleFactor

                retriever.getScaledFrameAtTime(
                    timeMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    targetWidth,
                    targetHeight
                )
            }else {
                retriever.getFrameAtTime(
                    timeMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
            }
            if (bitmap != null) {
                frame = SampledFrame(timeMs, bitmap)
            }

        } finally {
            retriever.release()
        }

        frame
    }
}
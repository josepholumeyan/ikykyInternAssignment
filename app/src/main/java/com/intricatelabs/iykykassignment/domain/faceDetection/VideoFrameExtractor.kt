package com.intricatelabs.iykykassignment.domain.faceDetection

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Pulls sampled frames out of a video at a fixed interval.
 */
class VideoFrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun extractFrames(
        retriever: MediaMetadataRetriever,
        timeMs: Long
    ): SampledFrame? = withContext(Dispatchers.Default) {
        var frame: SampledFrame? = null

        try {
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
            } else {
                retriever.getFrameAtTime(
                    timeMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
            }
            if (bitmap != null) {
                frame = SampledFrame(timeMs, bitmap)
            }

        } catch (e: Exception) {
            Log.e("VideoFrameExtractor", "Error extracting frame at $timeMs", e)
        }

        frame
    }
}

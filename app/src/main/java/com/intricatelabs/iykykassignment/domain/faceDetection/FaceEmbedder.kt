package com.intricatelabs.iykykassignment.domain.faceDetection

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceEmbedder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        const val MODEL_ASSET_NAME = "mobilefacenet.tflite"
    }
    private val inputSize = 112
    private val embeddingSize = 192

    private val interpreter: Interpreter =
        Interpreter(FileUtil.loadMappedFile(context, MODEL_ASSET_NAME))

    fun embed(faceCrop: Bitmap): FloatArray {
        val resized = faceCrop.scale(inputSize, inputSize)
        val inputBuffer = bitmapToByteBuffer(resized)

        val output = Array(1) { FloatArray(embeddingSize) }
        interpreter.run(inputBuffer, output)

        return l2Normalize(output[0])
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 128f) // R
            buffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 128f)  // G
            buffer.putFloat(((pixel and 0xFF) - 127.5f) / 128f)        // B
        }
        return buffer
    }

    // Cosine similarity between two normalized embeddings reduces to a dot product
    private fun l2Normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        return if (norm > 0f) FloatArray(vector.size) { vector[it] / norm } else vector
    }

    fun close() = interpreter.close()
}

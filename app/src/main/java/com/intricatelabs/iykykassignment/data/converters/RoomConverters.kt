package com.intricatelabs.iykykassignment.data.converters

import android.graphics.Rect
import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromRect(rect: Rect): String {
        return "${rect.left},${rect.top},${rect.right},${rect.bottom}"
    }

    @TypeConverter
    fun toRect(rectString: String): Rect {
        val parts = rectString.split(",")
        return Rect(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt(),
            parts[3].toInt()
        )
    }

    @TypeConverter
    fun fromFloatArray(array: FloatArray): String {
        return array.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(arrayString: String): FloatArray {
        return arrayString.split(",").map { it.toFloat() }.toFloatArray()
    }
}

package com.intricatelabs.iykykassignment.domain.collage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Composes representative shots into one shareable image. Plain
 * android.graphics — no extra dependency justified for a one-shot final
 * render that runs once per video, not per-frame.
 *
 * Layout note: this lays people out in a simple adaptive square grid, NOT
 * the mockup's hand-tuned overlapping-circle arrangement — same trade-off
 * as ResultsContent's grid: scales to any person count instead of being
 * hardcoded for 5. If you want pixel parity with the mockup, this is the
 * file to replace with a custom five-slot layout later.
 */
object CollageComposer {
    private const val CELL_SIZE = 480
    private const val PADDING = 16
    private const val CORNER_RADIUS = 32f
    private val BACKGROUND_COLOR = "#0A0A0A".toColorInt()
    private val BADGE_BG_COLOR = "#99000000".toColorInt()
    private val ACCENT_COLOR = "#E0A94A".toColorInt()

    fun compose(people: List<PersonForCollage>): Bitmap {
        require(people.isNotEmpty()) { "Cannot compose a collage with zero people" }

        val columns = ceil(sqrt(people.size.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(people.size.toDouble() / columns).toInt()

        val width = columns * CELL_SIZE + (columns + 1) * PADDING
        val height = rows * CELL_SIZE + (rows + 1) * PADDING

        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        canvas.drawColor(BACKGROUND_COLOR)

        people.forEachIndexed { index, person ->
            val col = index % columns
            val row = index / columns
            val left = (PADDING + col * (CELL_SIZE + PADDING)).toFloat()
            val top = (PADDING + row * (CELL_SIZE + PADDING)).toFloat()
            drawPersonTile(canvas, person, left, top)
        }

        return output
    }

    private fun drawPersonTile(canvas: Canvas, person: PersonForCollage, left: Float, top: Float) {
        val source = BitmapFactory.decodeFile(person.imagePath) ?: return
        val cropped = centerCropToSquare(source)
        val scaled = cropped.scale(CELL_SIZE, CELL_SIZE)

        // Rounded-corner clip — a raw square grid dump doesn't read as a
        // "designed collage" the way the mockup's rounded tiles do.
        val rect = RectF(left, top, left + CELL_SIZE, top + CELL_SIZE)
        val path = Path().apply { addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(scaled, left, top, null)
        canvas.restore()

        drawAppearanceBadge(canvas, person.appearanceCount, left, top)

        if (cropped !== source) source.recycle()
        cropped.recycle()
        scaled.recycle()
    }

    private fun centerCropToSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    private fun drawAppearanceBadge(canvas: Canvas, count: Int, tileLeft: Float, tileTop: Float) {
        val text = "$count appearance${if (count == 1) "" else "s"}"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT_COLOR
            textSize = 28f
            isFakeBoldText = true
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BADGE_BG_COLOR }

        val padding = 12f
        val textWidth = textPaint.measureText(text)
        val badgeLeft = tileLeft + 12f
        val badgeTop = tileTop + 12f
        val badgeRect = RectF(
            badgeLeft, badgeTop,
            badgeLeft + textWidth + padding * 2, badgeTop + textPaint.textSize + padding
        )
        canvas.drawRoundRect(badgeRect, 12f, 12f, bgPaint)
        canvas.drawText(text, badgeLeft + padding, badgeTop + textPaint.textSize, textPaint)
    }
}

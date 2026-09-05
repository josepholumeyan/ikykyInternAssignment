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
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.scale

/**
 * Composes representative shots with a simple visual hierarchy instead of a
 * flat uniform grid: whoever appears most gets a full-width "hero" tile up
 * top, everyone else fills an adaptive grid below. One flexible layout
 * rather than a hand-tuned template per exact person count — scales to any
 * N while still reading as designed, not just "a grid of data."
 */
object CollageComposer {

    private const val PADDING = 24
    private const val CORNER_RADIUS = 44f
    private const val GRID_CELL_SIZE = 420
    private const val HERO_HEIGHT = 640
    private const val MAX_GRID_COLUMNS = 3

    private val BACKGROUND_TOP = "#141210".toColorInt()
    private val BACKGROUND_BOTTOM = "#000000".toColorInt()
    private val ACCENT_COLOR = "#E0A94A".toColorInt()

    fun compose(people: List<PersonForCollage>): Bitmap {
        require(people.isNotEmpty()) { "Cannot compose a collage with zero people" }

        val sorted = people.sortedByDescending { it.appearanceCount }
        val hero = sorted.first()
        val rest = sorted.drop(1)

        val gridColumns = if (rest.isEmpty()) 0
        else ceil(sqrt(rest.size.toDouble())).toInt().coerceIn(1, MAX_GRID_COLUMNS)
        val gridRows = if (rest.isEmpty()) 0
        else ceil(rest.size.toDouble() / gridColumns).toInt()

        val gridContentWidth = gridColumns * GRID_CELL_SIZE + (gridColumns + 1) * PADDING
        val canvasWidth = if (rest.isEmpty()) GRID_CELL_SIZE + 2 * PADDING else gridContentWidth
        val canvasHeight = PADDING + HERO_HEIGHT + PADDING +
                (gridRows * GRID_CELL_SIZE) + (if (gridRows > 0) (gridRows + 1) * PADDING else 0)

        val output = createBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(output)
        drawBackground(canvas, canvasWidth, canvasHeight)

        // Hero tile — full width, taller, whoever appeared most.
        drawTile(
            canvas, hero,
            left = PADDING.toFloat(),
            top = PADDING.toFloat(),
            width = (canvasWidth - PADDING * 2).toFloat(),
            height = HERO_HEIGHT.toFloat(),
            isHero = true
        )

        // Grid — everyone else.
        rest.forEachIndexed { index, person ->
            val col = index % gridColumns
            val row = index / gridColumns
            val left = PADDING + col * (GRID_CELL_SIZE + PADDING)
            val top = PADDING + HERO_HEIGHT + PADDING + row * (GRID_CELL_SIZE + PADDING)
            drawTile(
                canvas, person,
                left = left.toFloat(), top = top.toFloat(),
                width = GRID_CELL_SIZE.toFloat(), height = GRID_CELL_SIZE.toFloat(),
                isHero = false
            )
        }

        return output
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                BACKGROUND_TOP, BACKGROUND_BOTTOM, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun drawTile(
        canvas: Canvas,
        person: PersonForCollage,
        left: Float, top: Float, width: Float, height: Float,
        isHero: Boolean
    ) {
        val source = BitmapFactory.decodeFile(person.imagePath) ?: return
        val cropped = centerCropToAspect(source, width / height)
        val scaled = cropped.scale(width.toInt(), height.toInt())

        try {
            val rect = RectF(left, top, left + width, top + height)
            val path = Path().apply { addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW) }

            canvas.withClip(path) {
                drawBitmap(scaled, left, top, null)
                val scrimPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, top + height * 0.55f, 0f, top + height,
                        Color.TRANSPARENT, "#CC000000".toColorInt(), Shader.TileMode.CLAMP
                    )
                }
                drawRect(left, top + height * 0.55f, left + width, top + height, scrimPaint)
            }

            drawAppearanceLabel(canvas, person.appearanceCount, left, top, width, height, isHero)
        }  finally {
            if (cropped !== source) source.recycle()
            cropped.recycle()
            scaled.recycle()
        }
    }

    private fun centerCropToAspect(bitmap: Bitmap, targetAspect: Float): Bitmap {
        val currentAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (currentAspect > targetAspect) {
            val newWidth = (bitmap.height * targetAspect).toInt().coerceAtLeast(1)
            val x = (bitmap.width - newWidth) / 2
            Bitmap.createBitmap(bitmap, x, 0, newWidth, bitmap.height)
        } else {
            val newHeight = (bitmap.width / targetAspect).toInt().coerceAtLeast(1)
            val y = (bitmap.height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, y, bitmap.width, newHeight)
        }
    }

    private fun drawAppearanceLabel(
        canvas: Canvas, count: Int,
        left: Float, top: Float, width: Float, height: Float,
        isHero: Boolean
    ) {
        val text = "$count appearance${if (count == 1) "" else "s"}"
        val textSize = if (isHero) 44f else 30f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            isFakeBoldText = true
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT_COLOR
            this.textSize = textSize * 0.7f
            isFakeBoldText = true
        }

        val textX = left + 24f
        val textY = top + height - 28f

        canvas.drawText(text, textX, textY, textPaint)
        if (isHero) {
            canvas.drawText("MOST FEATURED", textX, textY - textSize - 8f, accentPaint)
        }
    }
}
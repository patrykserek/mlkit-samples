package com.google.mlkit.md.objectdetection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.md.camera.GraphicOverlay
import com.google.mlkit.vision.objects.DetectedObject
import kotlin.math.max
import kotlin.math.min

class ObjectGraphic(
    overlay: GraphicOverlay,
    private val detectedObject: DetectedObject
) : GraphicOverlay.Graphic(overlay) {

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        isAntiAlias = true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun draw(canvas: Canvas) {
        // Draws arcs at the corners of the bounding box.
        val rect = RectF(detectedObject.boundingBox)
        val x0 = overlay.translateX(rect.left)
        val x1 = overlay.translateX(rect.right)
        rect.left = min(x0, x1)
        rect.right = max(x0, x1)
        rect.top = overlay.translateY(rect.top)
        rect.bottom = overlay.translateY(rect.bottom)

        // Top-left corner arc
        canvas.drawArc(
            rect.left - STROKE_WIDTH,
            rect.top - STROKE_WIDTH,
            rect.left + CORNER_LENGTH,
            rect.top + CORNER_LENGTH,
            180f,
            90f,
            false,
            cornerPaint,
        )

        // Top-right corner arc
        canvas.drawArc(
            rect.right - CORNER_LENGTH,
            rect.top - STROKE_WIDTH / 2,
            rect.right + STROKE_WIDTH / 2,
            rect.top + CORNER_LENGTH,
            270f,
            90f,
            false,
            cornerPaint,
        )

        // Bottom-left corner arc
        canvas.drawArc(
            rect.left - STROKE_WIDTH / 2,
            rect.bottom - CORNER_LENGTH,
            rect.left + CORNER_LENGTH,
            rect.bottom + STROKE_WIDTH / 2,
            90f,
            90f,
            false,
            cornerPaint,
        )

        // Bottom-right corner arc
        canvas.drawArc(
            rect.right - CORNER_LENGTH,
            rect.bottom - CORNER_LENGTH,
            rect.right + STROKE_WIDTH / 2,
            rect.bottom + STROKE_WIDTH / 2,
            0f,
            90f,
            false,
            cornerPaint,
        )
    }

    companion object {
        private const val STROKE_WIDTH = 12.0f
        private const val CORNER_LENGTH = 120f
    }
}

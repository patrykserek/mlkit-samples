package com.google.mlkit.md.objectdetection

import com.google.mlkit.md.camera.GraphicOverlay
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.objects.DetectedObject
import java.util.Locale

/** Draw the detected object info in preview. */
class ObjectRectGraphic(
    overlay: GraphicOverlay,
    private val detectedObject: DetectedObject,
) : GraphicOverlay.Graphic(overlay) {

    companion object {
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
        private const val NUM_COLORS = 10
        private val COLORS = arrayOf(
            intArrayOf(Color.BLACK, Color.WHITE),
            intArrayOf(Color.WHITE, Color.MAGENTA),
            intArrayOf(Color.BLACK, Color.LTGRAY),
            intArrayOf(Color.WHITE, Color.RED),
            intArrayOf(Color.WHITE, Color.BLUE),
            intArrayOf(Color.WHITE, Color.DKGRAY),
            intArrayOf(Color.BLACK, Color.CYAN),
            intArrayOf(Color.BLACK, Color.YELLOW),
            intArrayOf(Color.WHITE, Color.BLACK),
            intArrayOf(Color.BLACK, Color.GREEN)
        )
        private const val LABEL_FORMAT = "%.2f%% confidence (index: %d)"
    }

    private val boxPaints: Array<Paint>
    private val textPaints: Array<Paint>
    private val labelPaints: Array<Paint>

    init {
        val numColors = COLORS.size
        textPaints = Array(numColors) { Paint() }
        boxPaints = Array(numColors) { Paint() }
        labelPaints = Array(numColors) { Paint() }
        for (i in 0 until numColors) {
            textPaints[i].apply {
                color = COLORS[i][0] // text color
                textSize = TEXT_SIZE
            }

            boxPaints[i].apply {
                color = COLORS[i][1] // background color
                style = Paint.Style.STROKE
                strokeWidth = STROKE_WIDTH
            }

            labelPaints[i].apply {
                color = COLORS[i][1] // background color
                style = Paint.Style.FILL
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Decide color based on object tracking ID
        val colorID = detectedObject.trackingId?.let { Math.abs(it % NUM_COLORS) } ?: 0
        val textPaint = textPaints[colorID]
        val boxPaint = boxPaints[colorID]
        val labelPaint = labelPaints[colorID]

        var textWidth = textPaint.measureText("Tracking ID: ${detectedObject.trackingId}")
        val lineHeight = TEXT_SIZE + STROKE_WIDTH
        var yLabelOffset = -lineHeight

        // Calculate width and height of label box
        for (label in detectedObject.labels) {
            textWidth = maxOf(textWidth, textPaint.measureText(label.text))
            textWidth = maxOf(
                textWidth,
                textPaint.measureText(
                    String.format(Locale.US, LABEL_FORMAT, label.confidence * 100, label.index)
                )
            )
            yLabelOffset -= 2 * lineHeight
        }

        // Draws the bounding box.
        val rect = RectF(detectedObject.boundingBox)
        // If the image is flipped, the left will be translated to right, and the right to left.
        val x0 = overlay.translateX(rect.left)
        val x1 = overlay.translateX(rect.right)
        rect.left = minOf(x0, x1)
        rect.right = maxOf(x0, x1)
        rect.top = overlay.translateY(rect.top)
        rect.bottom = overlay.translateY(rect.bottom)
        canvas.drawRect(rect, boxPaint)

        // Draws other object info.
        canvas.drawRect(
            rect.left - STROKE_WIDTH,
            rect.top + yLabelOffset,
            rect.left + textWidth + 2 * STROKE_WIDTH,
            rect.top,
            labelPaint
        )
        yLabelOffset += TEXT_SIZE
        canvas.drawText(
            "Tracking ID: ${detectedObject.trackingId}",
            rect.left,
            rect.top + yLabelOffset,
            textPaint
        )
        yLabelOffset += lineHeight

        for (label in detectedObject.labels) {
            canvas.drawText(label.text, rect.left, rect.top + yLabelOffset, textPaint)
            yLabelOffset += lineHeight
            canvas.drawText(
                String.format(Locale.US, LABEL_FORMAT, label.confidence * 100, label.index),
                rect.left,
                rect.top + yLabelOffset,
                textPaint
            )
            yLabelOffset += lineHeight
        }
    }
}

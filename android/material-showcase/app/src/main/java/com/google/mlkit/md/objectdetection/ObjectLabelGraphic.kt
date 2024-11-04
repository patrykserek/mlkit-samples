package com.google.mlkit.md.objectdetection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.mlkit.md.camera.GraphicOverlay
import com.google.mlkit.md.camera.GraphicOverlay.Graphic
import com.google.mlkit.vision.objects.DetectedObject

internal class ObjectLabelGraphic(
    overlay: GraphicOverlay,
    private val visionObject: DetectedObject,
    private val labelText: String,
    private val textSize: Float = 40f,
    private val textColor: Int = Color.WHITE
) : Graphic(overlay) {

    private val textPaint: Paint = Paint().apply {
        color = textColor
        isAntiAlias = true
        textSize = this@ObjectLabelGraphic.textSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    override fun draw(canvas: Canvas) {
        val rect = overlay.translateRect(visionObject.boundingBox)
        // Determine the position where the text should be drawn (centered)
        val helfRectWidth = (rect.right - rect.left) / 2
        val helfRectHeight = (rect.bottom - rect.top) / 2
        val cx = rect.left + helfRectWidth//canvas.width / 2f
        val cy = rect.top + helfRectHeight//canvas.height / 2f
        
        // Adjust the text position if you want it slightly above or below the object
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2

        // Draw the text label centered on the canvas
        canvas.drawText(labelText, cx, textY, textPaint)
    }
}

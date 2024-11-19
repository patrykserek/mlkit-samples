package com.google.mlkit.md

import android.graphics.Rect

fun Rect.overlaps(other: Rect, maxIntersectionOverUnion: Float = 0.1f): Boolean =
    this.intersectionOverUnion(other) >= maxIntersectionOverUnion

fun Rect.intersectionOverUnion(other: Rect): Float {
    // Find intersection rectangle
    val intersection = Rect()
    val intersects = intersection.setIntersect(this, other)

    if (!intersects || intersection.isEmpty) {
        return 0.0f
    }

    // Calculate intersection area
    val intersectionArea = intersection.width() * intersection.height()

    // Calculate union area
    val thisArea = this.width() * this.height()
    val otherArea = other.width() * other.height()
    val unionArea = thisArea + otherArea - intersectionArea

    // Calculate Intersection over Union
    return intersectionArea.toFloat() / unionArea.toFloat()
}

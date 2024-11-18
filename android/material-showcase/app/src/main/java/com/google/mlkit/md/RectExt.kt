package com.google.mlkit.md

import android.graphics.Rect

fun Rect.overlaps(other: Rect, overlapFactor: Float = 0.2f): Boolean {
    if (overlapFactor < 0 || overlapFactor > 1) {
        throw IllegalArgumentException("Overlap factor must be between 0 and 1")
    }

    // Calculate the intersection rectangle
    val intersectLeft = maxOf(left, other.left)
    val intersectRight = minOf(right, other.right)
    val intersectTop = maxOf(top, other.top)
    val intersectBottom = minOf(bottom, other.bottom)

    // If there is no intersection, return false
    if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) {
        return false
    }

    // Calculate the area of each rectangle
    val intersectionArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
    val thisArea = (right - left) * (bottom - top)
    val otherArea = (other.right - other.left) * (other.bottom - other.top)

    // Determine the minimum overlap area based on the overlap factor
    val minOverlapArea = overlapFactor * minOf(thisArea, otherArea)

    // Check if the intersection area meets the minimum overlap requirement
    return intersectionArea >= minOverlapArea
}

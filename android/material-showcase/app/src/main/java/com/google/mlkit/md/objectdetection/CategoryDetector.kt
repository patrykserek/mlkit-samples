package com.google.mlkit.md.objectdetection

import android.graphics.Bitmap
import com.google.mlkit.md.classification.ImageSearcher

class CategoryDetector(
    private val imageSearcher: ImageSearcher,
) {
    private val detectedCategories: MutableMap<Int, String> = mutableMapOf()

    suspend fun getCategory(objectId: Int, image: Bitmap): String? {
        return if (detectedCategories[objectId] == null) {
            detectedCategories[objectId] = "LOADING"
            val detectedCategory = imageSearcher.searchWithImage(image)
            detectedCategories[objectId] = detectedCategory
            detectedCategory
        } else detectedCategories[objectId]
    }
}
